package dev.typetype.android.services

import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.PositionInfo
import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackRepository
import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import dev.typetype.android.domain.stream.accept
import dev.typetype.android.data.network.PlaybackNetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal class SabrPlaybackServiceBridge(
    private val player: Player,
    repository: SabrPlaybackRepository,
    private val windowCache: SabrPlaybackWindowCache,
    private val playbackClock: SabrPlaybackClock,
    private val recoveryDispatcher: SabrPlaybackRecoveryDispatcher,
    private val networkMonitor: PlaybackNetworkMonitor,
) : Player.Listener, SabrPlaybackRecoveryDispatcher.Listener, AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val coordinator = SabrPlaybackSeekCoordinator(
        repository = repository,
        scope = scope,
        currentState = { player.currentMediaItem?.sabrPlaybackSeekState() },
        apply = ::applySession,
    )
    private val recoveryGate = SabrPlaybackRecoveryGate()
    private val networkRecoveryGate = PlaybackNetworkRecoveryGate()
    private val liveFollower = LivePlaybackFollower()
    private var activeBinding: SabrPlaybackBinding? = null
    private var networkRetryJob: Job? = null

    fun setAudioOnly(enabled: Boolean, complete: (Result<Unit>) -> Unit) {
        val state = player.currentMediaItem?.sabrPlaybackSeekState()
        if (state == null) {
            complete(Result.failure(AudioOnlyInactivePlaybackFailure()))
            return
        }
        if (state.target.isLive) {
            complete(Result.failure(AudioOnlyUnavailableFailure()))
            return
        }
        if (state.target.audioOnly == enabled) {
            complete(Result.success(Unit))
            return
        }
        coordinator.switchAudioOnly(
            state = state,
            enabled = enabled,
            positionMs = currentSabrMediaTimeMs(state) ?: player.currentPosition,
            complete = complete,
        )
    }

    init {
        player.addListener(this)
        recoveryDispatcher.setListener(this)
        scope.launch {
            networkMonitor.states.drop(1).collect { state ->
                scheduleNetworkRecovery(networkRecoveryGate.networkChanged(state))
            }
        }
        scope.launch {
            while (true) {
                val state = player.currentMediaItem?.sabrPlaybackSeekState()
                val positionMs = if (state == null) {
                    player.currentPosition
                } else {
                    currentSabrMediaTimeMs(state)
                }
                positionMs?.let {
                    playbackClock.update(it, player.playbackParameters.speed)
                }
                if (player.isPlaying && positionMs != null) {
                    recoveryGate.observeProgress(
                        mediaId = player.currentMediaItem?.mediaId,
                        positionMs = positionMs,
                        nowMs = SystemClock.elapsedRealtime(),
                    )
                }
                followLiveEdge()
                delay(PLAYBACK_CLOCK_INTERVAL_MS)
            }
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val nextState = mediaItem?.sabrPlaybackSeekState()
        val transitionPositionMs = if (nextState?.liveActive == true) {
            mediaItem.requestMetadata.extras?.resumePositionMillis()
        } else {
            player.currentPosition
        }
        transitionPositionMs?.let {
            playbackClock.update(it, player.playbackParameters.speed)
        }
        coordinator.cancel()
        val extras = mediaItem?.requestMetadata?.extras
        val nextBinding = extras?.sabrPlaybackBinding()
        networkRecoveryGate.transition(mediaItem?.mediaId)
        liveFollower.transition(mediaItem?.mediaId)
        networkRetryJob?.cancel()
        networkRetryJob = null
        recoveryGate.transition(
            mediaId = mediaItem?.mediaId,
            startsNewSession = startsNewSabrPlaybackSession(
                previous = activeBinding,
                next = nextBinding,
                continuesCurrentSession = extras?.getBoolean(
                    MergedStreamMediaKeys.EXTRA_SABR_SESSION_CONTINUATION,
                ) == true,
            ),
        )
        activeBinding = nextBinding
    }

    override fun onPlayerError(error: PlaybackException) {
        if (!error.isRecoverableSabrSessionFailure()) {
            if (error.isNetworkPlaybackFailure()) {
                val mediaId = player.currentMediaItem?.mediaId ?: return
                scheduleNetworkRecovery(
                    networkRecoveryGate.failed(mediaId, networkMonitor.snapshot()),
                )
            }
            return
        }
        val state = player.currentMediaItem?.sabrPlaybackSeekState() ?: return
        val replacement = error.sabrSessionReplacementFailure()
        val recovery = error.sabrPlaybackRecoveryFailure()
        val recoveryTarget = if (recovery == null) {
            state.target
        } else {
            state.target.recoveryTarget(recovery) ?: return
        }
        val replacementTarget = replacement?.let {
            runCatching { state.target.accept(it.session) }.getOrNull() ?: return
        }
        val sessionId = state.binding.sessionId
        when (recoveryGate.begin(state.mediaId, sessionId)) {
            SabrPlaybackRecoveryDecision.Ignore,
            SabrPlaybackRecoveryDecision.Exhausted,
            -> return
            SabrPlaybackRecoveryDecision.Recover -> Unit
        }
        if (replacement != null && replacementTarget != null) {
            try {
                applySession(
                    replacement.session,
                    replacementTarget,
                    currentSabrMediaTimeMs(state) ?: 0L,
                )
            } finally {
                recoveryGate.finish(sessionId)
            }
            return
        }
        coordinator.recoverBounded(
            state = state,
            target = recoveryTarget,
            positionMs = currentSabrMediaTimeMs(state) ?: 0L,
            initialFailure = error,
            takeAttempt = recoveryGate::takeAttempt,
        ) {
            recoveryGate.finish(sessionId)
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (
            playbackState != Player.STATE_READY ||
            !networkMonitor.snapshot().isAvailable
        ) {
            return
        }
        networkRetryJob?.cancel()
        networkRetryJob = null
        networkRecoveryGate.recovered()
    }

    override fun onPositionDiscontinuity(
        oldPosition: PositionInfo,
        newPosition: PositionInfo,
        reason: Int,
    ) {
        if (reason != Player.DISCONTINUITY_REASON_SEEK) return
        liveFollower.observeSeek(
            currentMediaId = player.currentMediaItem?.mediaId,
            positionMs = newPosition.positionMs,
            targetMs = player.currentLiveTargetPositionMs(),
        )
    }

    override fun onRecoveryRequired(
        sessionId: String,
        failure: dev.typetype.android.data.stream.SabrPlaybackRecoveryException,
        complete: (Result<Unit>) -> Unit,
    ) {
        val state = player.currentMediaItem?.sabrPlaybackSeekState()
        if (state == null || state.binding.sessionId != sessionId) {
            complete(Result.success(Unit))
            return
        }
        val target = state.target.recoveryTarget(failure)
        if (target == null) {
            complete(Result.failure(failure))
            return
        }
        when (recoveryGate.begin(state.mediaId, sessionId)) {
            SabrPlaybackRecoveryDecision.Ignore -> complete(Result.success(Unit))
            SabrPlaybackRecoveryDecision.Exhausted -> complete(Result.failure(failure))
            SabrPlaybackRecoveryDecision.Recover -> coordinator.recoverBounded(
                state = state,
                target = target,
                positionMs = currentSabrMediaTimeMs(state) ?: 0L,
                initialFailure = failure,
                takeAttempt = recoveryGate::takeAttempt,
            ) { result ->
                recoveryGate.finish(sessionId)
                complete(result)
            }
        }
    }

    private fun applySession(
        session: SabrPlaybackSession,
        target: SabrPlaybackTarget,
        positionMs: Long,
    ) {
        val currentItem = player.currentMediaItem ?: return
        val playWhenReady = player.playWhenReady
        windowCache.put(session)
        val replacement = currentItem.withSabrPlayback(session, target)
        player.setMediaItem(
            replacement,
            sabrWindowPositionMs(
                mediaTimeMs = positionMs,
                liveActive = session.live?.active == true,
                seekableStartMs = session.live?.seekableStartMs ?: 0L,
                seekableEndMs = session.live?.seekableEndMs ?: 0L,
            ),
        )
        player.prepare()
        player.playWhenReady = playWhenReady
    }

    private fun scheduleNetworkRecovery(action: PlaybackNetworkRecoveryAction) {
        if (action !is PlaybackNetworkRecoveryAction.RetryAfter) return
        val mediaId = player.currentMediaItem?.mediaId ?: return
        networkRetryJob?.cancel()
        networkRetryJob = scope.launch {
            delay(action.delayMs)
            if (
                networkRecoveryGate.isPending(mediaId) &&
                player.currentMediaItem?.mediaId == mediaId
            ) {
                restartInterruptedPlayback()
            }
        }
    }

    private fun restartInterruptedPlayback() {
        if (player.playbackState == Player.STATE_READY) {
            networkRecoveryGate.recovered()
            return
        }
        if (player.playbackState !in NETWORK_RECOVERY_STATES) return
        val playWhenReady = player.playWhenReady
        player.stop()
        player.prepare()
        player.playWhenReady = playWhenReady
    }

    private fun currentSabrMediaTimeMs(state: SabrPlaybackSeekState): Long? =
        player.sabrMediaTimeMs(player.currentPosition, state.liveActive)

    private fun followLiveEdge() {
        val mediaId = player.currentMediaItem?.mediaId
        val targetMs = player.currentLiveTargetPositionMs()
        liveFollower.initialize(mediaId, player.currentPosition, targetMs)
        liveFollower.nextTarget(
            currentMediaId = mediaId,
            positionMs = player.currentPosition,
            targetMs = targetMs,
            playing = player.isPlaying,
            busy = player.playbackState != Player.STATE_READY || player.isLoading,
            nowMs = SystemClock.elapsedRealtime(),
        )?.let(player::seekTo)
    }

    override fun close() {
        player.removeListener(this)
        recoveryDispatcher.setListener(null)
        networkRetryJob?.cancel()
        networkRetryJob = null
        coordinator.cancel()
        scope.cancel()
    }

    private companion object {
        const val PLAYBACK_CLOCK_INTERVAL_MS = 250L
        val NETWORK_RECOVERY_STATES = setOf(Player.STATE_IDLE, Player.STATE_BUFFERING)
    }
}
