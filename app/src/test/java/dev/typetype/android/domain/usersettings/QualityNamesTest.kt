package dev.typetype.android.domain.usersettings

import org.junit.Assert.assertEquals
import org.junit.Test

class QualityNamesTest {
    @Test
    fun normalizesRawServerLabelsToHeightOnly() {
        assertEquals("1080p", normalizeQualityName("1080p60 Premium"))
        assertEquals("720p", normalizeQualityName(" Medium · 720p30 "))
    }

    @Test
    fun preservesExplicitHighDynamicRange() {
        assertEquals("2160p HDR", normalizeQualityName("2160p HDR"))
    }

    @Test
    fun providesSensibleFallback() {
        assertEquals("1080p", normalizeQualityName(""))
    }
}
