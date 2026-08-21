package dev.typetype.android.feature.player.host

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerHostTransitionTest {
    @Test
    fun playerSizeAndPositionFollowTheDrag() {
        val transition = playerHostTransition(
            offsetPx = 350f,
            miniAnchorPx = 700f,
            containerHeightPx = 800f,
            miniHeightPx = 64f,
            isAnimationRunning = false,
        )

        assertEquals(0.5f, transition.progress)
        assertEquals(432, transition.heightPx)
        assertEquals(350, transition.offsetPx)
        assertFalse(transition.isSettledMini)
    }

    @Test
    fun miniPresentationWaitsForTheMotionToSettle() {
        val moving = playerHostTransition(700f, 700f, 800f, 64f, true)
        val settled = playerHostTransition(700f, 700f, 800f, 64f, false)

        assertFalse(moving.isSettledMini)
        assertTrue(settled.isSettledMini)
        assertEquals(64, settled.heightPx)
    }

    @Test
    fun invalidOffsetUsesTheExpandedPresentation() {
        val transition = playerHostTransition(Float.NaN, 700f, 800f, 64f, false)

        assertEquals(0f, transition.progress)
        assertEquals(800, transition.heightPx)
        assertEquals(0, transition.offsetPx)
    }
}
