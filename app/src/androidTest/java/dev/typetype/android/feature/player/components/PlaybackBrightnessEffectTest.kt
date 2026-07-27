package dev.typetype.android.feature.player.components

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PlaybackBrightnessEffectTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val fullscreen = mutableStateOf(false)
    private val selectedPercent = mutableStateOf<Int?>(null)

    @Before
    fun showEffect() {
        composeRule.activity.window.clearPlaybackBrightnessOverride()
        composeRule.setContent {
            PlaybackBrightnessEffect(
                window = composeRule.activity.window,
                isFullscreen = fullscreen.value,
                selectedPercent = selectedPercent.value,
            )
        }
    }

    @After
    fun clearOverride() {
        composeRule.runOnIdle {
            composeRule.activity.window.clearPlaybackBrightnessOverride()
        }
    }

    @Test
    fun selectedBrightnessIsRestoredAfterLeavingFullscreen() {
        composeRule.runOnIdle {
            selectedPercent.value = 60
            fullscreen.value = true
        }
        composeRule.waitForIdle()
        assertWindowBrightness(0.6f)

        composeRule.runOnIdle {
            fullscreen.value = false
        }
        composeRule.waitForIdle()
        assertWindowBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)

        composeRule.runOnIdle {
            fullscreen.value = true
        }
        composeRule.waitForIdle()
        assertWindowBrightness(0.6f)
    }

    private fun assertWindowBrightness(expected: Float) {
        composeRule.runOnIdle {
            assertEquals(
                expected,
                composeRule.activity.window.attributes.screenBrightness,
                0.001f,
            )
        }
    }
}
