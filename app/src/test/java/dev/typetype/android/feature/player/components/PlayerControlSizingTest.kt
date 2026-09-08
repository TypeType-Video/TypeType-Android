package dev.typetype.android.feature.player.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerControlSizingTest {
    @Test fun `landscape phone never uses tablet controls`() {
        assertFalse(useExpandedPlayerControls(411, 891f, 411f))
        assertFalse(useExpandedPlayerControls(360, 800f, 360f))
    }

    @Test fun `tablet keeps larger controls when player has room`() {
        assertTrue(useExpandedPlayerControls(800, 800f, 450f))
        assertTrue(useExpandedPlayerControls(800, 1280f, 800f))
    }

    @Test fun `small tablet viewport stays compact`() {
        assertFalse(useExpandedPlayerControls(800, 500f, 281f))
    }
}
