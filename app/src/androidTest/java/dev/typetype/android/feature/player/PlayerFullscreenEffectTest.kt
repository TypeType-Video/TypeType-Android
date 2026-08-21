package dev.typetype.android.feature.player

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.test.filters.SdkSuppress
import dev.typetype.android.core.ui.util.WindowHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PlayerFullscreenEffectTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<FullscreenTestActivity>()

    private val fullscreen = mutableStateOf(false)

    @Before
    fun showEffectInPortrait() {
        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        composeRule.waitUntil(ORIENTATION_TIMEOUT_MILLIS) {
            composeRule.activity.resources.configuration.orientation ==
                Configuration.ORIENTATION_PORTRAIT
        }
        composeRule.waitUntil(ORIENTATION_TIMEOUT_MILLIS) {
            composeRule.activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        composeRule.setContent {
            PlayerFullscreenEffect(
                activity = composeRule.activity,
                isFullscreen = fullscreen.value,
                locksLandscape = true,
            )
        }
        composeRule.waitForIdle()
    }

    @After
    fun restoreWindow() {
        composeRule.runOnIdle { fullscreen.value = false }
        composeRule.waitForIdle()
        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    @Test
    @SdkSuppress(maxSdkVersion = 36)
    fun fullscreenRotatesAndRestoresTheWindow() {
        composeRule.runOnIdle { fullscreen.value = true }

        composeRule.waitUntil(ORIENTATION_TIMEOUT_MILLIS) {
            composeRule.activity.resources.configuration.orientation ==
                Configuration.ORIENTATION_LANDSCAPE
        }
        composeRule.waitUntil(ORIENTATION_TIMEOUT_MILLIS) {
            composeRule.activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        composeRule.runOnIdle {
            assertEquals(
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
                composeRule.activity.requestedOrientation,
            )
            assertTrue(hasNoLimitsFlag())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                assertEquals(
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES,
                    composeRule.activity.window.attributes.layoutInDisplayCutoutMode,
                )
            }
        }
        composeRule.waitUntil(SYSTEM_BARS_TIMEOUT_MILLIS) { !systemBarsVisible() }

        composeRule.runOnIdle { fullscreen.value = false }

        composeRule.waitUntil(ORIENTATION_TIMEOUT_MILLIS) {
            composeRule.activity.resources.configuration.orientation ==
                Configuration.ORIENTATION_PORTRAIT
        }
        composeRule.waitUntil(ORIENTATION_TIMEOUT_MILLIS) {
            composeRule.activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        composeRule.runOnIdle {
            assertEquals(
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                composeRule.activity.requestedOrientation,
            )
            assertFalse(hasNoLimitsFlag())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                assertEquals(
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT,
                    composeRule.activity.window.attributes.layoutInDisplayCutoutMode,
                )
            }
        }
        composeRule.waitUntil(SYSTEM_BARS_TIMEOUT_MILLIS) { systemBarsVisible() }
    }

    @Test
    fun fullscreenWindowStateRestoresWithoutRotation() {
        composeRule.runOnIdle {
            WindowHelper.toggleFullscreen(composeRule.activity.window, isFullscreen = true)
        }
        composeRule.waitUntil(SYSTEM_BARS_TIMEOUT_MILLIS) { !systemBarsVisible() }
        composeRule.runOnIdle {
            assertTrue(hasNoLimitsFlag())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                assertEquals(
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES,
                    composeRule.activity.window.attributes.layoutInDisplayCutoutMode,
                )
            }
        }

        composeRule.runOnIdle {
            WindowHelper.toggleFullscreen(composeRule.activity.window, isFullscreen = false)
        }
        composeRule.waitUntil(SYSTEM_BARS_TIMEOUT_MILLIS) { systemBarsVisible() }
        composeRule.runOnIdle {
            assertFalse(hasNoLimitsFlag())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                assertEquals(
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT,
                    composeRule.activity.window.attributes.layoutInDisplayCutoutMode,
                )
            }
        }
    }

    private fun hasNoLimitsFlag(): Boolean =
        composeRule.activity.window.attributes.flags and
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS != 0

    @Suppress("DEPRECATION")
    private fun systemBarsVisible(): Boolean {
        val decorView = composeRule.activity.window.decorView
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val hiddenFlags = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            return decorView.systemUiVisibility and hiddenFlags == 0
        }
        val insets = ViewCompat.getRootWindowInsets(decorView) ?: return false
        return insets.isVisible(WindowInsetsCompat.Type.statusBars()) &&
            insets.isVisible(WindowInsetsCompat.Type.navigationBars())
    }

    private companion object {
        const val ORIENTATION_TIMEOUT_MILLIS = 15_000L
        const val SYSTEM_BARS_TIMEOUT_MILLIS = 5_000L
    }
}
