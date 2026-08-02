package dev.typetype.android.feature.player.components

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlaybackKeepScreenOnEffectTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val videoIsPlaying = mutableStateOf(false)

    @After
    fun clearFlag() {
        composeRule.runOnIdle {
            composeRule.activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    @Test
    fun screenStaysAwakeOnlyWhileVideoIsPlaying() {
        composeRule.setContent {
            PlaybackKeepScreenOnEffect(
                window = composeRule.activity.window,
                videoIsPlaying = videoIsPlaying.value,
            )
        }
        assertKeepScreenOn(expected = false)

        composeRule.runOnIdle { videoIsPlaying.value = true }
        composeRule.waitForIdle()
        assertKeepScreenOn(expected = true)

        composeRule.runOnIdle { videoIsPlaying.value = false }
        composeRule.waitForIdle()
        assertKeepScreenOn(expected = false)
    }

    private fun assertKeepScreenOn(expected: Boolean) {
        composeRule.runOnIdle {
            val enabled = composeRule.activity.window.attributes.flags and
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
            if (expected) assertTrue(enabled) else assertFalse(enabled)
        }
    }
}
