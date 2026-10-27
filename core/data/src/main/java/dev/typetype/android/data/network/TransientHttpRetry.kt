package dev.typetype.android.data.network

import java.time.DateTimeException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun isTransientHttpStatus(statusCode: Int): Boolean =
    statusCode in TRANSIENT_HTTP_STATUS_CODES

fun transientHttpRetryDelayMs(
    errorCount: Int,
    maximumRetries: Int,
    requestedDelayMs: Long?,
): Long? {
    if (errorCount > maximumRetries) return null
    requestedDelayMs?.let { return it.coerceIn(MIN_REQUESTED_RETRY_DELAY_MS, MAX_REQUESTED_RETRY_DELAY_MS) }
    val exponent = (errorCount - 1).coerceIn(0, 3)
    return (DEFAULT_TRANSIENT_RETRY_MS * (1L shl exponent)).coerceAtMost(MAX_TRANSIENT_RETRY_DELAY_MS)
}

fun Map<String, List<String>>.retryAfterMillis(
    nowMs: Long = System.currentTimeMillis(),
): Long? {
    val value = entries
        .firstOrNull { it.key.equals(RETRY_AFTER_HEADER, ignoreCase = true) }
        ?.value
        ?.firstOrNull()
        ?.trim()
        ?: return null
    value.toLongOrNull()?.takeIf { it >= 0L }?.let {
        return it.coerceAtMost(Long.MAX_VALUE / 1_000L) * 1_000L
    }
    return try {
        (ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
            .toInstant()
            .toEpochMilli() - nowMs).coerceAtLeast(0L)
    } catch (_: DateTimeException) {
        null
    } catch (_: ArithmeticException) {
        null
    }
}

private val TRANSIENT_HTTP_STATUS_CODES = setOf(429, 502, 503, 504)
private const val RETRY_AFTER_HEADER = "Retry-After"
private const val DEFAULT_TRANSIENT_RETRY_MS = 500L
private const val MAX_TRANSIENT_RETRY_DELAY_MS = 3_000L
private const val MIN_REQUESTED_RETRY_DELAY_MS = 250L
private const val MAX_REQUESTED_RETRY_DELAY_MS = 30_000L
