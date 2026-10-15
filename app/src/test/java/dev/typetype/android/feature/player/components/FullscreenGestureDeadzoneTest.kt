package dev.typetype.android.feature.player.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FullscreenGestureDeadzoneTest {
    @Test
    fun identifiesTopAndBottomDeadZones() {
        assertTrue(isWithinFullscreenGestureDeadzone(8f, 600f, 48f))
        assertTrue(isWithinFullscreenGestureDeadzone(592f, 600f, 48f))
        assertFalse(isWithinFullscreenGestureDeadzone(120f, 600f, 48f))
    }

    @Test
    fun deadzoneCannotConsumeTheEntireShortViewport() {
        assertTrue(isWithinFullscreenGestureDeadzone(20f, 60f, 48f))
        assertFalse(isWithinFullscreenGestureDeadzone(30f, 60f, 0f))
        assertFalse(isWithinFullscreenGestureDeadzone(30f, 0f, 48f))
    }
}
