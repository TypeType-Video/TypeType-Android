package dev.typetype.android.services

import dev.typetype.android.data.network.PlaybackNetworkState

internal sealed interface PlaybackRecoveryDecision {
    data class RetryAfter(val delayMs: Long) : PlaybackRecoveryDecision

    data object WaitForNetwork : PlaybackRecoveryDecision

    data object GiveUp : PlaybackRecoveryDecision
}

// Decides, for the current media item, whether a failed playback is restarted and when. The recovery window opens
// at the first failure and only closes after a stretch of clean playback, so a stream that keeps failing between
// short successes still ends on an error instead of retrying forever.
internal class PlaybackRecoveryTracker(
    private val policy: PlaybackRecoveryPolicy = PlaybackRecoveryPolicy(),
    private val clock: () -> Long,
) {
    private var mediaId: String? = null
    private var windowStartMs: Long? = null
    private var attempt = 0
    private var waitingForNetwork = false

    fun transition(nextMediaId: String?) {
        if (nextMediaId == mediaId) return
        mediaId = nextMediaId
        reset()
    }

    fun failed(failedMediaId: String, network: PlaybackNetworkState): PlaybackRecoveryDecision {
        transition(failedMediaId)
        val nowMs = clock()
        val startMs = windowStartMs ?: nowMs.also { windowStartMs = it }
        if (policy.isExhausted(nowMs - startMs)) return PlaybackRecoveryDecision.GiveUp
        if (!network.isAvailable) {
            waitingForNetwork = true
            return PlaybackRecoveryDecision.WaitForNetwork
        }
        attempt++
        return PlaybackRecoveryDecision.RetryAfter(policy.delayMs(attempt))
    }

    // A new route ends the wait at once; the attempt counts toward the same window.
    fun networkReturned(): Boolean {
        if (!waitingForNetwork) return false
        waitingForNetwork = false
        attempt++
        return true
    }

    fun stablePlayback(stableMs: Long) {
        if (windowStartMs != null && stableMs >= STABLE_PLAYBACK_RESET_MS) reset()
    }

    private fun reset() {
        windowStartMs = null
        attempt = 0
        waitingForNetwork = false
    }

    private companion object {
        const val STABLE_PLAYBACK_RESET_MS = 30_000L
    }
}
