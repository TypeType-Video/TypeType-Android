package dev.typetype.android.services

import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackRepository
import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import dev.typetype.android.domain.stream.accept
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class SabrPlaybackServiceBridge(
    private val player: Player,
    repository: SabrPlaybackRepository,
    private val windowCache: SabrPlaybackWindowCache,
    private val playbackClock: SabrPlaybackClock,
    private val recoveryDispatcher: SabrPlaybackRecoveryDispatcher,
) : Player.Listener, SabrPlaybackRecoveryDispatcher.Listener, AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val coordinator = SabrPlaybackSeekCoordinator(
        repository = repository,
        scope = scope,
        currentState = { player.currentMediaItem?.sabrPlaybackSeekState() },
        apply = ::applySession,
    )
    private val recoveryGate = SabrPlaybackRecoveryGate()
    private var activeBinding: SabrPlaybackBinding? = null

    init {
        player.addListener(this)
        recoveryDispatcher.setListener(this)
        scope.launch {
            while (true) {
                val positionMs = player.currentPosition
                playbackClock.update(positionMs, player.playbackParameters.speed)
                if (player.isPlaying) {
                    recoveryGate.observeProgress(
                        mediaId = player.currentMediaItem?.mediaId,
                        positionMs = positionMs,
                        nowMs = SystemClock.elapsedRealtime(),
                    )
                }
                delay(PLAYBACK_CLOCK_INTERVAL_MS)
            }
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        playbackClock.update(player.currentPosition, player.playbackParameters.speed)
        coordinator.cancel()
        val extras = mediaItem?.requestMetadata?.extras
        val nextBinding = extras?.sabrPlaybackBinding()
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
        if (!error.isRecoverableSabrSessionFailure()) return
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
                applySession(replacement.session, replacementTarget, player.currentPosition)
            } finally {
                recoveryGate.finish(sessionId)
            }
            return
        }
        coordinator.recoverBounded(
            state = state,
            target = recoveryTarget,
            positionMs = player.currentPosition,
            initialFailure = error,
            takeAttempt = recoveryGate::takeAttempt,
        ) {
            recoveryGate.finish(sessionId)
        }
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
                positionMs = player.currentPosition,
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
        player.setMediaItem(replacement, positionMs)
        player.prepare()
        player.playWhenReady = playWhenReady
    }

    override fun close() {
        player.removeListener(this)
        recoveryDispatcher.setListener(null)
        coordinator.cancel()
        scope.cancel()
    }

    private companion object {
        const val PLAYBACK_CLOCK_INTERVAL_MS = 250L
    }
}
