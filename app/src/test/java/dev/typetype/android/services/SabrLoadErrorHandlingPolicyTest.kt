package dev.typetype.android.services

import androidx.media3.common.C
import dev.typetype.android.data.network.transientHttpRetryDelayMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SabrLoadErrorHandlingPolicyTest {
    @Test
    fun `segment readiness polling is bounded to sixty requests`() {
        assertEquals(250L, sabrSegmentRetryDelayMs(1, 250L))
        assertEquals(250L, sabrSegmentRetryDelayMs(59, 250L))
        assertEquals(C.TIME_UNSET, sabrSegmentRetryDelayMs(60, 250L))
    }

    @Test
    fun `transient network backoff matches the frontend budget`() {
        assertEquals(500L, transientHttpRetryDelayMs(1, null))
        assertEquals(1_000L, transientHttpRetryDelayMs(2, null))
        assertEquals(2_000L, transientHttpRetryDelayMs(3, null))
        assertEquals(3_000L, transientHttpRetryDelayMs(4, null))
        assertEquals(3_000L, transientHttpRetryDelayMs(24, null))
        assertNull(transientHttpRetryDelayMs(25, null))
    }

    @Test
    fun `server retry delay is parsed and clamped`() {
        val body = """{"status":"preparing","retryAfterMs":250}""".toByteArray()

        assertEquals(250L, body.sabrResponseRetryAfterMs())
        assertEquals(100L, sabrSegmentRetryDelayMs(1, 0L))
        assertEquals(3_000L, transientHttpRetryDelayMs(1, 30_000L))
        assertNull("""{"status":"preparing"}""".toByteArray().sabrResponseRetryAfterMs())
    }
}
