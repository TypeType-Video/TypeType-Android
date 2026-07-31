package dev.typetype.android.feature.player.components

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerTimeBarGeometryTest {
    @Test
    fun zeroWidthDuringPipTransitionKeepsThumbAtOrigin() {
        val startX = playerTimeBarThumbStartX(
            progressX = 0f,
            trackWidth = 0f,
            thumbWidth = 42f,
        )

        assertEquals(0f, startX)
    }

    @Test
    fun trackNarrowerThanThumbKeepsThumbAtOrigin() {
        val startX = playerTimeBarThumbStartX(
            progressX = 10f,
            trackWidth = 20f,
            thumbWidth = 42f,
        )

        assertEquals(0f, startX)
    }

    @Test
    fun ordinaryTrackCentersAndClampsThumb() {
        assertEquals(29f, playerTimeBarThumbStartX(50f, 100f, 42f))
        assertEquals(0f, playerTimeBarThumbStartX(0f, 100f, 42f))
        assertEquals(58f, playerTimeBarThumbStartX(100f, 100f, 42f))
    }
}
