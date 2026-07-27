package dev.typetype.android.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SabrPlayerTimelineTest {
    @Test
    fun `live window positions are translated to the server media timeline`() {
        assertEquals(
            55_000L,
            sabrMediaTimeMs(
                windowPositionMs = 10_000L,
                expectedLive = true,
                placeholder = false,
                timelineLive = true,
                positionInFirstPeriodMs = 45_000L,
            ),
        )
    }

    @Test
    fun `static and non live targets keep their player position`() {
        assertEquals(
            10_000L,
            sabrMediaTimeMs(10_000L, true, false, false, 45_000L),
        )
        assertEquals(
            10_000L,
            sabrMediaTimeMs(10_000L, false, false, true, 45_000L),
        )
    }

    @Test
    fun `a placeholder live timeline is not reported to the server`() {
        assertNull(sabrMediaTimeMs(10_000L, true, true, true, 45_000L))
    }

    @Test
    fun `live position translation cannot overflow`() {
        assertEquals(
            Long.MAX_VALUE,
            sabrMediaTimeMs(Long.MAX_VALUE, true, false, true, 1L),
        )
    }
}
