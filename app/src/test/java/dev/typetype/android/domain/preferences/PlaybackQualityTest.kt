package dev.typetype.android.domain.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackQualityTest {
    @Test
    fun `auto keeps the highest quality on unmetered networks`() {
        assertEquals(
            "2160p",
            effectivePlaybackQualityCap(
                defaultQuality = "auto",
                mobileDataQuality = "1080p",
                isMetered = false,
            ),
        )
    }

    @Test
    fun `auto uses the mobile cap on metered networks`() {
        assertEquals(
            "1080p",
            effectivePlaybackQualityCap(
                defaultQuality = "auto",
                mobileDataQuality = "1080p",
                isMetered = true,
            ),
        )
    }

    @Test
    fun `a lower default quality stays below the mobile cap`() {
        assertEquals(
            "720p",
            effectivePlaybackQualityCap(
                defaultQuality = "720p",
                mobileDataQuality = "1080p",
                isMetered = true,
            ),
        )
    }

    @Test
    fun `a higher default quality is capped on mobile`() {
        assertEquals(
            "1080p",
            effectivePlaybackQualityCap(
                defaultQuality = "2160p",
                mobileDataQuality = "1080p",
                isMetered = true,
            ),
        )
    }

    @Test
    fun `quality names accept hdr suffixes`() {
        assertEquals("1440p", normalizePlaybackQuality("1440p HDR"))
    }
}
