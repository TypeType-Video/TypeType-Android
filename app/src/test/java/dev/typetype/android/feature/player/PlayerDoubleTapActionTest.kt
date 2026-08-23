package dev.typetype.android.feature.player

import dev.typetype.android.feature.player.components.PlayerDoubleTapAction
import dev.typetype.android.feature.player.components.doubleTapAction
import dev.typetype.android.feature.player.components.isEnabled
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerDoubleTapActionTest {
    @Test
    fun `double tap divides the player into three stable zones`() {
        assertEquals(PlayerDoubleTapAction.Rewind, doubleTapAction(0f, 900f))
        assertEquals(PlayerDoubleTapAction.Rewind, doubleTapAction(299f, 900f))
        assertEquals(PlayerDoubleTapAction.TogglePlayback, doubleTapAction(300f, 900f))
        assertEquals(PlayerDoubleTapAction.TogglePlayback, doubleTapAction(600f, 900f))
        assertEquals(PlayerDoubleTapAction.Forward, doubleTapAction(601f, 900f))
        assertEquals(PlayerDoubleTapAction.Forward, doubleTapAction(900f, 900f))
    }

    @Test
    fun `center playback toggle remains available when seek gestures are disabled`() {
        assertFalse(PlayerDoubleTapAction.Rewind.isEnabled(seekEnabled = false))
        assertTrue(PlayerDoubleTapAction.TogglePlayback.isEnabled(seekEnabled = false))
        assertFalse(PlayerDoubleTapAction.Forward.isEnabled(seekEnabled = false))
    }
}
