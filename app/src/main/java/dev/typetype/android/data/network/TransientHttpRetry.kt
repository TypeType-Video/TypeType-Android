package dev.typetype.android.data.network

import java.time.DateTimeException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal fun isTransientHttpStatus(statusCode: Int): Boolean =
    statusCode in TRANSIENT_HTTP_STATUS_CODES

internal fun transientHttpRetryDelayMs(
    errorCount: Int,
    requestedDelayMs: Long?,
): Long? {
    if (errorCount > MAX_TRANSIENT_RETRIES) return null
    val exponent = (errorCount - 1).coerceIn(0, 3)
    val backoffMs = DEFAULT_TRANSIENT_RETRY_MS * (1L shl exponent)
    return (requestedDelayMs ?: backoffMs).coerceIn(0L, MAX_TRANSIENT_RETRY_DELAY_MS)
}

internal fun Map<String, List<String>>.retryAfterMillis(
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

private val TRANSIENT_HTTP_STATUS_CODES = setOf(502, 503, 504)
private const val RETRY_AFTER_HEADER = "Retry-After"
private const val DEFAULT_TRANSIENT_RETRY_MS = 500L
private const val MAX_TRANSIENT_RETRY_DELAY_MS = 3_000L
private const val MAX_TRANSIENT_RETRIES = 24
