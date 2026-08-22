package dev.typetype.android.feature.player.components

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerLevelGestureTest {
    @Test
    fun `upward drag raises the level gradually`() {
        assertEquals(0.75f, adjustLevelFraction(0.5f, -150f, 600f), 0.001f)
    }

    @Test
    fun `level stays inside the supported range`() {
        assertEquals(1f, adjustLevelFraction(0.9f, -500f, 300f), 0.001f)
        assertEquals(0f, adjustLevelFraction(0.1f, 500f, 300f), 0.001f)
    }

    @Test
    fun `short drag range cannot divide by zero`() {
        assertEquals(0f, adjustLevelFraction(0.5f, 1f, 0f), 0.001f)
    }
}
