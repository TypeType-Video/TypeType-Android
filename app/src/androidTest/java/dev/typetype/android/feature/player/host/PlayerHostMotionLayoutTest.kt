package dev.typetype.android.feature.player.host

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.cancel
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlayerHostMotionLayoutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun playerFollowsDownwardDragIntoMiniPlayer() {
        val observedProgress = mutableStateListOf<Float>()
        var settledTarget by mutableStateOf(PlayerHostTarget.Expanded)
        var requestStamp by mutableIntStateOf(0)
        var dragDistancePx = 0f
        composeRule.setContent {
            val density = LocalDensity.current
            dragDistancePx = with(density) { 500.dp.toPx() }
            Box(Modifier.requiredWidth(400.dp).requiredHeight(600.dp)) {
                PlayerHostMotionLayout(
                    target = settledTarget,
                    requestStamp = requestStamp.toLong(),
                    miniAnchorPx = dragDistancePx,
                    containerHeightPx = with(density) { 600.dp.toPx() },
                    miniHeightPx = with(density) { 64.dp.toPx() },
                    dragEnabled = true,
                    miniContentEnabled = true,
                    onTargetSettled = {
                        settledTarget = it
                        requestStamp += 1
                    },
                    onProgressChange = observedProgress::add,
                    miniContent = { Text("Mini content") },
                    expandedContent = { Text("Expanded content", modifier = it) },
                )
            }
        }

        composeRule.onNodeWithTag(PLAYER_HOST_OVERLAY_TAG).performTouchInput {
            swipe(center, Offset(center.x, bottom - 1f), 600L)
        }
        composeRule.waitUntil(5_000) { settledTarget == PlayerHostTarget.Mini }
        composeRule.onNodeWithText("Mini content").assertIsDisplayed()
        assertTrue(observedProgress.any { it in 0.1f..0.9f })
    }

    @Test
    fun miniPlayerFollowsUpwardDragIntoExpandedPlayer() {
        val observedProgress = mutableStateListOf<Float>()
        var settledTarget by mutableStateOf(PlayerHostTarget.Mini)
        var requestStamp by mutableIntStateOf(0)
        composeRule.setContent {
            val density = LocalDensity.current
            Box(Modifier.requiredWidth(400.dp).requiredHeight(600.dp)) {
                PlayerHostMotionLayout(
                    target = settledTarget,
                    requestStamp = requestStamp.toLong(),
                    miniAnchorPx = with(density) { 500.dp.toPx() },
                    containerHeightPx = with(density) { 600.dp.toPx() },
                    miniHeightPx = with(density) { 64.dp.toPx() },
                    dragEnabled = true,
                    miniContentEnabled = true,
                    onTargetSettled = {
                        settledTarget = it
                        requestStamp += 1
                    },
                    onProgressChange = observedProgress::add,
                    miniContent = { Text("Mini content") },
                    expandedContent = { Text("Expanded content", modifier = it) },
                )
            }
        }

        composeRule.onNodeWithText("Mini content").assertIsDisplayed()
        observedProgress.clear()
        composeRule.onNodeWithText("Mini content").performTouchInput {
            swipe(center, Offset(center.x, center.y - 300f), 100L)
        }
        assertTrue(
            "Upward drag did not move the player: $observedProgress",
            observedProgress.any { it < 0.99f },
        )
        composeRule.waitUntil(5_000) { settledTarget == PlayerHostTarget.Expanded }
        composeRule.onNodeWithText("Expanded content").assertIsDisplayed()
    }

    @Test
    fun cancelledDragReturnsWithoutSettlingOrRestartingPlaybackState() {
        val observedProgress = mutableStateListOf<Float>()
        val settledTargets = mutableStateListOf<PlayerHostTarget>()
        var miniAnchorPx = 0f
        composeRule.setContent {
            val density = LocalDensity.current
            miniAnchorPx = with(density) { 500.dp.toPx() }
            Box(Modifier.requiredWidth(400.dp).requiredHeight(600.dp)) {
                PlayerHostMotionLayout(
                    target = PlayerHostTarget.Expanded,
                    requestStamp = 7,
                    miniAnchorPx = miniAnchorPx,
                    containerHeightPx = with(density) { 600.dp.toPx() },
                    miniHeightPx = with(density) { 64.dp.toPx() },
                    dragEnabled = true,
                    miniContentEnabled = true,
                    onTargetSettled = settledTargets::add,
                    onProgressChange = observedProgress::add,
                    miniContent = { Text("Mini content") },
                    expandedContent = { Text("Expanded content", modifier = it) },
                )
            }
        }
        composeRule.waitForIdle()
        settledTargets.clear()
        observedProgress.clear()

        composeRule.onNodeWithTag(PLAYER_HOST_OVERLAY_TAG).performTouchInput {
            down(center)
            moveTo(Offset(center.x, center.y + miniAnchorPx * 0.35f), 300L)
            cancel()
        }

        assertTrue(observedProgress.any { it in 0.1f..0.9f })
        composeRule.waitUntil(5_000) { observedProgress.lastOrNull()?.let { it < 0.01f } == true }
        assertTrue(settledTargets.isEmpty())
        composeRule.onNodeWithText("Expanded content").assertIsDisplayed()
    }

    @Test
    fun disabledDragDoesNotMoveThePlayer() {
        val observedProgress = mutableStateListOf<Float>()
        composeRule.setContent {
            val density = LocalDensity.current
            Box(Modifier.requiredWidth(400.dp).requiredHeight(600.dp)) {
                PlayerHostMotionLayout(
                    target = PlayerHostTarget.Expanded,
                    requestStamp = 9,
                    miniAnchorPx = with(density) { 500.dp.toPx() },
                    containerHeightPx = with(density) { 600.dp.toPx() },
                    miniHeightPx = with(density) { 64.dp.toPx() },
                    dragEnabled = false,
                    miniContentEnabled = false,
                    onTargetSettled = {},
                    onProgressChange = observedProgress::add,
                    miniContent = { Text("Mini content") },
                    expandedContent = { Text("Expanded content", modifier = it) },
                )
            }
        }
        composeRule.waitForIdle()
        observedProgress.clear()

        composeRule.onNodeWithTag(PLAYER_HOST_OVERLAY_TAG).performTouchInput {
            swipe(center, Offset(center.x, bottom - 1f), 200L)
        }

        assertTrue(observedProgress.all { it < 0.01f })
        composeRule.onNodeWithText("Expanded content").assertIsDisplayed()
    }
}
