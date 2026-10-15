package dev.typetype.android.feature.player.components

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.typetype.android.feature.player.state.PlayerGestureState
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FullscreenGestureDeadzoneIntegrationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun edgeTouchesPassThroughAndCenterGesturesRemainActive() {
        val player = GestureTestPlayer(Looper.getMainLooper())
        val tapCount = AtomicInteger()
        composeRule.setContent {
            PlayerGestureLayer(
                player = player,
                state = remember { PlayerGestureState() },
                onSingleTap = { tapCount.incrementAndGet() },
                onAdjustBrightness = {},
                onAdjustVolume = {},
                isFullscreen = true,
                config = PlayerGestureConfig(swipeSeekEnabled = true),
                modifier = Modifier.size(300.dp, 180.dp).testTag(GESTURE_TAG),
            )
        }

        composeRule.onNodeWithTag(GESTURE_TAG).performTouchInput {
            click(Offset(center.x, 8f))
            doubleClick(Offset(center.x, height - 8f))
            swipe(Offset(40f, 8f), Offset(width - 40f, 8f), 300L)
            swipe(Offset(40f, height - 8f), Offset(width - 40f, height - 8f), 300L)
        }
        composeRule.runOnIdle {
            assertEquals(0, tapCount.get())
            assertEquals(20_000L, player.currentPosition)
        }

        composeRule.onNodeWithTag(GESTURE_TAG).performTouchInput {
            click(center)
            swipe(Offset(40f, center.y), Offset(width - 40f, center.y), 300L)
        }
        composeRule.runOnIdle {
            assertEquals(1, tapCount.get())
            assertTrue(player.currentPosition > 20_000L)
            player.release()
        }
    }

    @Test
    fun fullscreenExitIgnoresTopAndBottomEdgeSwipes() {
        val exitCount = AtomicInteger()
        composeRule.setContent {
            PlayerFullscreenExitGestureBox(
                enabled = true,
                onGestureFeedback = {},
                onExitFullscreen = { exitCount.incrementAndGet() },
                modifier = Modifier.size(300.dp, 180.dp).testTag(GESTURE_TAG),
            ) { Box(Modifier.fillMaxSize()) }
        }

        composeRule.onNodeWithTag(GESTURE_TAG).performTouchInput {
            swipe(Offset(center.x, 8f), Offset(center.x, height - 8f), 300L)
            swipe(Offset(center.x, height - 8f), Offset(center.x, 8f), 300L)
        }
        composeRule.runOnIdle { assertEquals(0, exitCount.get()) }

        composeRule.onNodeWithTag(GESTURE_TAG).performTouchInput {
            swipe(center, Offset(center.x, height - 8f), 300L)
        }
        composeRule.runOnIdle { assertEquals(1, exitCount.get()) }
    }

    private companion object {
        const val GESTURE_TAG = "fullscreen_gesture_deadzone"
    }
}
