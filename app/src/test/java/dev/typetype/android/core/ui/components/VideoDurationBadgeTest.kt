package dev.typetype.android.core.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoDurationBadgeTest {
    @Test
    fun `formats short and long videos consistently`() {
        assertEquals("0:05", formatVideoDuration(5))
        assertEquals("1:02", formatVideoDuration(62))
        assertEquals("1:01:01", formatVideoDuration(3_661))
    }
}
