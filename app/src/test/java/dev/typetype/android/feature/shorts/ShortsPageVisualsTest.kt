package dev.typetype.android.feature.shorts

import org.junit.Assert.assertEquals
import org.junit.Test

class ShortsPageVisualsTest {
    @Test
    fun settledPageKeepsItsOverlayFullyVisible() {
        assertEquals(1f, shortsOverlayAlpha(0f), 0f)
        assertEquals(0f, shortsOverlayTranslationY(0f), 0f)
    }

    @Test
    fun overlayFollowsTheSwipeWithoutDisappearing() {
        assertEquals(0.69f, shortsOverlayAlpha(0.5f), 0.001f)
        assertEquals(12f, shortsOverlayTranslationY(0.5f), 0f)
    }

    @Test
    fun pageDistanceIsBounded() {
        assertEquals(shortsOverlayAlpha(1f), shortsOverlayAlpha(4f))
        assertEquals(shortsOverlayAlpha(0f), shortsOverlayAlpha(-1f))
    }
}
