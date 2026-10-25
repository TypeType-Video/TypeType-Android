package dev.typetype.android.services

import android.os.SystemClock
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import dev.typetype.android.data.network.PlaybackNetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

internal class PlaybackInterruptionRecovery(
    private val player: Player,
    private val coordinator: SabrPlaybackSeekCoordinator,
    private val networkMonitor: PlaybackNetworkMonitor,
    private val status: PlaybackRecoveryStatus,
    private val scope: CoroutineScope,
    private val tracker: PlaybackRecoveryTracker = PlaybackRecoveryTracker(clock = SystemClock::elapsedRealtime),
) {
    private var mediaId: String? = null
    private var retryJob: Job? = null

    init {
        scope.launch {
            networkMonitor.states.drop(1).collect { state ->
                if (state.isAvailable && tracker.networkReturned()) restart()
            }
        }
    }

    fun transition(nextMediaId: String?) {
        if (nextMediaId == mediaId) return
        mediaId = nextMediaId
        tracker.transition(nextMediaId)
        cancelRetry()
        status.clear()
    }

    fun failed(error: PlaybackException) {
        val failedMediaId = player.currentMediaItem?.mediaId ?: return
        if (error.playbackRecoveryCause() == PlaybackRecoveryCause.Fatal) {
            status.gaveUp(failedMediaId)
            return
        }
        retry(failedMediaId)
    }

    fun retry(failedMediaId: String) {
        when (val decision = tracker.failed(failedMediaId, networkMonitor.snapshot())) {
            is PlaybackRecoveryDecision.RetryAfter -> {
                status.recovering(failedMediaId)
                schedule(failedMediaId, decision.delayMs)
            }
            PlaybackRecoveryDecision.WaitForNetwork -> status.recovering(failedMediaId)
            PlaybackRecoveryDecision.GiveUp -> status.gaveUp(failedMediaId)
        }
    }

    fun ready() {
        cancelRetry()
        status.clear()
    }

    fun playing(stableMs: Long) = tracker.stablePlayback(stableMs)

    fun close() = cancelRetry()

    private fun schedule(failedMediaId: String, delayMs: Long) {
        cancelRetry()
        retryJob = scope.launch {
            delay(delayMs)
            if (player.currentMediaItem?.mediaId == failedMediaId) restart()
        }
    }

    private fun restart() {
        val failedMediaId = player.currentMediaItem?.mediaId ?: return
        if (player.playbackState == Player.STATE_READY) {
            ready()
            return
        }
        val state = player.currentMediaItem?.sabrPlaybackSeekState()
        if (state == null) {
            val playWhenReady = player.playWhenReady
            player.stop()
            player.prepare()
            player.playWhenReady = playWhenReady
            return
        }
        coordinator.recover(
            state = state,
            target = state.target,
            positionMs = player.currentSabrMediaTimeMs(state) ?: player.currentPosition,
        ) { result ->
            if (result.isFailure) retry(failedMediaId)
        }
    }

    private fun cancelRetry() {
        retryJob?.cancel()
        retryJob = null
    }
}
