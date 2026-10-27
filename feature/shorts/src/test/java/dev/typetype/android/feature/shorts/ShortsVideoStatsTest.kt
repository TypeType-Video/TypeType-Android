package dev.typetype.android.feature.shorts

import org.junit.Assert.assertEquals
import org.junit.Test

class ShortsVideoStatsTest {
    @Test
    fun `formats compact counts without hiding small values`() {
        assertEquals("0", formatShortsCount(-1))
        assertEquals("999", formatShortsCount(999))
        assertEquals("1.5K", formatShortsCount(1_500))
        assertEquals("2.0M", formatShortsCount(2_000_000))
    }
}
