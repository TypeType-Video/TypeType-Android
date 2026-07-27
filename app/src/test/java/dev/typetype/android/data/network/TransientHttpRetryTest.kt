package dev.typetype.android.data.network

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransientHttpRetryTest {
    @Test
    fun `transient backoff matches the web player retry budget`() {
        assertEquals(500L, transientHttpRetryDelayMs(1, null))
        assertEquals(1_000L, transientHttpRetryDelayMs(2, null))
        assertEquals(2_000L, transientHttpRetryDelayMs(3, null))
        assertEquals(3_000L, transientHttpRetryDelayMs(4, null))
        assertEquals(3_000L, transientHttpRetryDelayMs(24, null))
        assertNull(transientHttpRetryDelayMs(25, null))
    }

    @Test
    fun `retry after accepts seconds and ignores header casing`() {
        val headers = mapOf("retry-after" to listOf("2"))

        assertEquals(2_000L, headers.retryAfterMillis())
    }

    @Test
    fun `retry after accepts an HTTP date`() {
        val nowMs = 2_000_000_000_000L
        val date = DateTimeFormatter.RFC_1123_DATE_TIME.format(
            Instant.ofEpochMilli(nowMs + 2_000L).atZone(ZoneOffset.UTC),
        )

        assertEquals(2_000L, mapOf("Retry-After" to listOf(date)).retryAfterMillis(nowMs))
    }

    @Test
    fun `invalid retry after is ignored`() {
        assertNull(mapOf("Retry-After" to listOf("later")).retryAfterMillis())
    }

    @Test
    fun `large retry after cannot overflow into an immediate retry`() {
        val delay = mapOf(
            "Retry-After" to listOf(Long.MAX_VALUE.toString()),
        ).retryAfterMillis()

        assertEquals(3_000L, transientHttpRetryDelayMs(1, delay))
    }
}
