package dev.typetype.android.services

import android.os.SystemClock
import dev.typetype.android.data.network.PlaybackNetworkObserver
import dev.typetype.android.domain.stream.SabrPlaybackRepository
import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import kotlinx.coroutines.delay

// Keeps asking for the next window until the recovery window runs out, inside the media source: the player goes on
// with its buffer and media item, and nothing is shown unless the buffer runs dry. A lost or stalled session is
// replaced by a fresh one at the player position; a quality change still needs the service to rebuild the item.
internal class SabrWindowRecovery(
    private val repository: SabrPlaybackRepository,
    private val target: SabrPlaybackTarget,
    private val network: PlaybackNetworkObserver,
    private val policy: PlaybackRecoveryPolicy = PlaybackRecoveryPolicy(),
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val pause: suspend (Long) -> Unit = { delay(it) },
) {
    suspend fun run(
        positionMs: () -> Long,
        request: suspend () -> Result<SabrPlaybackSession>,
    ): Result<SabrPlaybackSession> {
        val startedAtMs = clock()
        var attempt = 0
        var result = request()
        while (true) {
            val cause = result.exceptionOrNull()?.playbackRecoveryCause() ?: return result
            if (cause == PlaybackRecoveryCause.Rebuild || cause == PlaybackRecoveryCause.Fatal) return result
            val elapsedMs = clock() - startedAtMs
            if (policy.isExhausted(elapsedMs)) return result
            attempt++
            if (!awaitRetry(attempt, policy.windowMs - elapsedMs)) return result
            result = if (cause == PlaybackRecoveryCause.Session) {
                repository.recoverOnce(target, positionMs())
            } else {
                request()
            }
        }
    }

    private suspend fun awaitRetry(attempt: Int, remainingMs: Long): Boolean {
        val state = network.snapshot()
        if (!state.isAvailable) return network.awaitAvailableAfter(state.generation, remainingMs)
        pause(policy.delayMs(attempt))
        return true
    }
}
