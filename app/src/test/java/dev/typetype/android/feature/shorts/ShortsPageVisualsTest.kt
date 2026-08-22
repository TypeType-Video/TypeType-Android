package dev.typetype.android.feature.shorts

import org.junit.Assert.assertEquals
import org.junit.Test

class ShortsPageVisualsTest {
    @Test
    fun settledPageKeepsItsOverlayFullyVisible() {
        val visuals = shortsPageVisuals(0f)

        assertEquals(1f, visuals.overlayAlpha, 0f)
        assertEquals(0f, visuals.overlayTranslationY, 0f)
    }

    @Test
    fun overlayFollowsTheSwipeWithoutDisappearing() {
        val visuals = shortsPageVisuals(0.5f)

        assertEquals(0.69f, visuals.overlayAlpha, 0.001f)
        assertEquals(12f, visuals.overlayTranslationY, 0f)
    }

    @Test
    fun pageDistanceIsBounded() {
        assertEquals(shortsPageVisuals(1f), shortsPageVisuals(4f))
        assertEquals(shortsPageVisuals(0f), shortsPageVisuals(-1f))
    }
}
