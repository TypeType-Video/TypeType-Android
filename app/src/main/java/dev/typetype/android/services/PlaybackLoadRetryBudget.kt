package dev.typetype.android.services

import android.os.SystemClock
import dev.typetype.android.data.network.PlaybackNetworkObserver
import dev.typetype.android.data.network.transientHttpRetryDelayMs

internal class PlaybackLoadRetryBudget(
    private val network: PlaybackNetworkObserver,
    private val elapsedRealtimeMs: () -> Long = SystemClock::elapsedRealtime,
) {
    private val attempts = mutableMapOf<Long, Attempt>()

    fun retryDelayMs(loadTaskId: Long): Long? = synchronized(attempts) {
        val state = network.snapshot()
        val nowMs = elapsedRealtimeMs()
        val previous = attempts[loadTaskId]
        val attempt = if (previous == null || previous.generation != state.generation) {
            Attempt(
                generation = state.generation,
                errorCount = 1,
                firstFailureAtMs = nowMs,
            )
        } else {
            previous.copy(errorCount = previous.errorCount + 1)
        }
        attempts[loadTaskId] = attempt
        if (!state.isAvailable) {
            OFFLINE_RETRY_DELAY_MS.takeIf {
                nowMs - attempt.firstFailureAtMs < MAX_OFFLINE_LOAD_WAIT_MS
            }
        } else {
            transientHttpRetryDelayMs(
                errorCount = attempt.errorCount,
                maximumRetries = MAX_CONNECTED_LOAD_RETRIES,
                requestedDelayMs = null,
            )
        }
    }

    fun conclude(loadTaskId: Long) {
        synchronized(attempts) {
            attempts.remove(loadTaskId)
        }
    }

    private data class Attempt(
        val generation: Long,
        val errorCount: Int,
        val firstFailureAtMs: Long,
    )
}

private const val MAX_CONNECTED_LOAD_RETRIES = 8
private const val OFFLINE_RETRY_DELAY_MS = 3_000L
private const val MAX_OFFLINE_LOAD_WAIT_MS = 10 * 60 * 1_000L
