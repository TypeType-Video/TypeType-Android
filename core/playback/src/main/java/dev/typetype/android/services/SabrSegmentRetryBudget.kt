package dev.typetype.android.services

import android.os.SystemClock

internal class SabrSegmentRetryBudget(
    private val elapsedRealtimeMs: () -> Long = SystemClock::elapsedRealtime,
    private val maximumRetries: Int = MAX_SABR_SEGMENT_RETRIES,
    private val maximumRetryDurationMs: Long = MAX_SEGMENT_RETRY_DURATION_MS,
) {
    private val firstFailures = mutableMapOf<Long, Long>()

    fun retryDelayMs(
        loadTaskId: Long,
        errorCount: Int,
        requestedDelayMs: Long?,
    ): Long? = synchronized(firstFailures) {
        if (errorCount > maximumRetries) return null
        val nowMs = elapsedRealtimeMs()
        val firstFailureMs = firstFailures.getOrPut(loadTaskId) { nowMs }
        val retryDelayMs = sabrSegmentRetryDelayMs(errorCount, requestedDelayMs)
        retryDelayMs.takeIf { nowMs - firstFailureMs + it <= maximumRetryDurationMs }
    }

    fun conclude(loadTaskId: Long) {
        synchronized(firstFailures) {
            firstFailures.remove(loadTaskId)
        }
    }
}

internal const val MAX_SABR_SEGMENT_RETRIES = 8
private const val MAX_SEGMENT_RETRY_DURATION_MS = 20_000L
