package dev.typetype.android

import dev.typetype.android.feature.player.host.PlayerHostTarget
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppChromeVisibilityTest {
    @Test
    fun hiddenAndMiniPlayersKeepAppChrome() {
        assertTrue(isAppChromeVisible(PlayerHostTarget.Hidden, false))
        assertTrue(isAppChromeVisible(PlayerHostTarget.Mini, false))
        assertTrue(isAppChromeVisible(PlayerHostTarget.Embedded, false))
    }

    @Test
    fun expandedAndFullscreenPlayersOwnTheWindow() {
        assertFalse(isAppChromeVisible(PlayerHostTarget.Expanded, false))
        assertFalse(isAppChromeVisible(PlayerHostTarget.Expanded, true))
        assertFalse(isAppChromeVisible(PlayerHostTarget.Mini, true))
    }
}
