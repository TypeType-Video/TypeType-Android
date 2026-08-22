package dev.typetype.android

import dev.typetype.android.feature.player.host.PlayerHostTarget
import org.junit.Assert.assertEquals
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

    @Test
    fun phoneChromeFollowsPlayerMotionInBothDirections() {
        assertEquals(
            0.35f,
            playerPhoneChromeAlpha(true, PlayerHostTarget.Expanded, false, 0.35f),
        )
        assertEquals(
            0.65f,
            playerPhoneChromeAlpha(true, PlayerHostTarget.Mini, false, 0.65f),
        )
    }

    @Test
    fun phoneChromeRemainsVisibleWithoutAnOverlayPlayer() {
        assertEquals(
            1f,
            playerPhoneChromeAlpha(false, PlayerHostTarget.Hidden, false, 0f),
        )
        assertEquals(
            1f,
            playerPhoneChromeAlpha(true, PlayerHostTarget.Embedded, false, 0f),
        )
    }
}
