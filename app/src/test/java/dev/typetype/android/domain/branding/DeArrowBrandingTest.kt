package dev.typetype.android.domain.branding

import org.junit.Assert.assertEquals
import org.junit.Test

class DeArrowBrandingTest {
    private val fallback = VideoBranding("Original title", "original.jpg")
    private val item = DeArrowItem(
        videoId = "dQw4w9WgXcQ",
        legacyTitle = "Legacy title",
        legacyThumbnailUrl = "legacy.jpg",
        titles = listOf(
            DeArrowTitleCandidate("Rejected", original = false, votes = -2, locked = false),
            DeArrowTitleCandidate("Accepted", original = false, votes = 1, locked = false),
            DeArrowTitleCandidate("Locked", original = false, votes = -1, locked = true),
        ),
        thumbnails = listOf(
            DeArrowThumbnailCandidate("rejected.jpg", original = false, votes = -2, locked = false),
            DeArrowThumbnailCandidate("accepted.jpg", original = false, votes = 0, locked = false),
        ),
        neutralThumbnailUrl = "neutral.jpg",
    )

    @Test
    fun `uses the first accepted community candidates`() {
        assertEquals(
            VideoBranding("Accepted", "accepted.jpg"),
            resolveDeArrowBranding(item, fallback, preferences()),
        )
    }

    @Test
    fun `supports original branding and locked only confidence`() {
        assertEquals(
            fallback,
            resolveDeArrowBranding(
                item,
                fallback,
                preferences(titleMode = "original", thumbnailMode = "original", trustMode = "locked"),
            ),
        )
        assertEquals(
            "Locked",
            resolveDeArrowBranding(item, fallback, preferences(trustMode = "locked")).title,
        )
    }

    @Test
    fun `uses a neutral frame according to thumbnail preferences`() {
        val withoutThumbnail = item.copy(thumbnails = emptyList())
        assertEquals(
            "neutral.jpg",
            resolveDeArrowBranding(
                withoutThumbnail,
                fallback,
                preferences(thumbnailMode = "random"),
            ).thumbnailUrl,
        )
        assertEquals(
            "neutral.jpg",
            resolveDeArrowBranding(
                withoutThumbnail,
                fallback,
                preferences(thumbnailMode = "dearrow_or_random"),
            ).thumbnailUrl,
        )
    }

    @Test
    fun `keeps originals when the accepted candidate is original`() {
        val originalFirst = item.copy(
            titles = listOf(
                DeArrowTitleCandidate("Original", original = true, votes = 0, locked = false),
            ),
            thumbnails = listOf(
                DeArrowThumbnailCandidate(null, original = true, votes = 0, locked = false),
            ),
        )
        assertEquals(fallback, resolveDeArrowBranding(originalFirst, fallback, preferences()))
    }

    @Test
    fun `supports legacy server payloads without candidate arrays`() {
        val legacy = item.copy(titles = null, thumbnails = null)
        assertEquals(
            VideoBranding("Legacy title", "legacy.jpg"),
            resolveDeArrowBranding(legacy, fallback, preferences()),
        )
    }

    private fun preferences(
        titleMode: String = "dearrow",
        thumbnailMode: String = "dearrow",
        trustMode: String = "accepted",
    ) = DeArrowPreferences(titleMode, thumbnailMode, trustMode)
}
