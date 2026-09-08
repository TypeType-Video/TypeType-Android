package dev.typetype.android.feature.player.components

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.typetype.android.feature.player.state.PlayerGestureState
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerControlVisibilityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun tapShowsControlsAfterAutomaticHideAndKeepsToggling() {
        val visible = mutableStateOf(true)
        val player = GestureTestPlayer(Looper.getMainLooper())
        val gestures = PlayerGestureState()
        composeRule.setContent {
            val currentVisibility = visible.value
            PlayerGestureLayer(
                player = player,
                state = gestures,
                onSingleTap = { visible.value = !currentVisibility },
                onAdjustBrightness = {},
                onAdjustVolume = {},
                isFullscreen = true,
                modifier = Modifier.size(600.dp, 300.dp).testTag("gesture"),
            )
        }
        tap()
        composeRule.runOnIdle { assertFalse(visible.value) }
        composeRule.runOnIdle { visible.value = true }
        composeRule.runOnIdle { visible.value = false }
        tap()
        composeRule.runOnIdle { assertTrue(visible.value) }
        tap()
        composeRule.runOnIdle { assertFalse(visible.value) }
        tap()
        composeRule.runOnIdle {
            assertTrue(visible.value)
            player.release()
        }
    }

    private fun tap() {
        composeRule.onNodeWithTag("gesture").performTouchInput {
            advanceEventTime(500L)
            click()
        }
        composeRule.mainClock.advanceTimeBy(500L)
        composeRule.waitForIdle()
    }
}
