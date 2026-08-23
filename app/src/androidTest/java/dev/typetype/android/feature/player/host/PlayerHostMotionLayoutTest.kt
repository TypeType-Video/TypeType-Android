package dev.typetype.android.feature.player.host

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.ui.PlayerView
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.typetype.android.feature.player.PlayerContentLayout
import dev.typetype.android.feature.player.PLAYER_VIEWPORT_TAG
import dev.typetype.android.feature.player.components.PlayerGestureLayer
import dev.typetype.android.feature.player.state.PlayerGestureState
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
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
        var crossedAnchors = 0
        var completedDrags = 0
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
                    onDragAnchorCrossed = { crossedAnchors += 1 },
                    onDragSettled = { completedDrags += 1 },
                    miniContent = { Text("Mini content") },
                    expandedContent = { Text("Expanded content") },
                )
            }
        }

        composeRule.onNodeWithTag(PLAYER_HOST_OVERLAY_TAG).performTouchInput {
            swipe(center, Offset(center.x, bottom - 1f), 600L)
        }
        composeRule.waitUntil(5_000) { settledTarget == PlayerHostTarget.Mini }
        composeRule.onNodeWithText("Mini content").assertIsDisplayed()
        assertTrue(observedProgress.any { it in 0.1f..0.9f })
        assertEquals(1, crossedAnchors)
        assertEquals(1, completedDrags)
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
                    expandedContent = { Text("Expanded content") },
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
                    expandedContent = { Text("Expanded content") },
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
                    expandedContent = { Text("Expanded content") },
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

    @Test
    fun realPlayerViewStaysAttachedFromFullscreenThroughMiniAndBack() {
        val viewportWidths = mutableStateListOf<Int>()
        val player = HostSurfacePlayer(Looper.getMainLooper())
        var target by mutableStateOf(PlayerHostTarget.Expanded)
        var isFullscreen by mutableStateOf(true)
        var requestStamp by mutableIntStateOf(0)
        var createdViews = 0
        var expandedWidthPx = 0
        var miniWidthPx = 0
        var miniPositionMs = 0L
        lateinit var firstView: PlayerView

        composeRule.setContent {
            val density = LocalDensity.current
            expandedWidthPx = with(density) { 400.dp.roundToPx() }
            miniWidthPx = with(density) { 80.dp.roundToPx() }
            Box(Modifier.requiredWidth(400.dp).requiredHeight(600.dp)) {
                PlayerHostMotionLayout(
                    target = target,
                    requestStamp = requestStamp.toLong(),
                    miniAnchorPx = with(density) { 500.dp.toPx() },
                    containerHeightPx = with(density) { 600.dp.toPx() },
                    miniHeightPx = with(density) { 64.dp.toPx() },
                    dragEnabled = true,
                    miniContentEnabled = true,
                    fullscreenCenterDragEnabled = isFullscreen,
                    onTargetSettled = {
                        target = it
                        if (it == PlayerHostTarget.Mini) isFullscreen = false
                        requestStamp += 1
                    },
                    onProgressChange = {},
                    miniContent = { Text("Mini controls") },
                    expandedContent = { transition ->
                        PlayerContentLayout(
                            isFullscreen = isFullscreen,
                            hostTransitionProgress = transition.progress,
                            modifier = Modifier.fillMaxSize(),
                            viewport = { modifier ->
                                Box(modifier) {
                                    AndroidView(
                                        factory = { context ->
                                            PlayerView(context).apply {
                                                useController = false
                                                this.player = player
                                                createdViews += 1
                                                if (createdViews == 1) firstView = this
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize().onSizeChanged {
                                            viewportWidths += it.width
                                        },
                                    )
                                    if (transition.progress < 0.01f) {
                                        PlayerGestureLayer(
                                            player = player,
                                            state = remember { PlayerGestureState() },
                                            onSingleTap = {},
                                            onAdjustBrightness = {},
                                            onAdjustVolume = {},
                                            isFullscreen = isFullscreen,
                                        )
                                    }
                                }
                            },
                            details = { Box(it.requiredHeight(300.dp)) },
                        )
                    },
                )
            }
        }

        composeRule.onNodeWithTag(PLAYER_HOST_OVERLAY_TAG).performTouchInput {
            swipe(center, Offset(center.x, bottom - 1f), 800L)
        }
        composeRule.waitUntil(5_000) { target == PlayerHostTarget.Mini }
        assertEquals(1, createdViews)
        composeRule.runOnIdle {
            assertSame(player, firstView.player)
            miniPositionMs = player.currentPosition
            assertTrue(miniPositionMs in 42_000L..60_000L)
        }
        assertTrue(viewportWidths.all { it in (expandedWidthPx - 2)..(expandedWidthPx + 2) })
        val miniVisualWidth = composeRule.onNodeWithTag(PLAYER_VIEWPORT_TAG)
            .fetchSemanticsNode().boundsInRoot.width
        assertTrue(miniVisualWidth in (miniWidthPx - 2f)..(miniWidthPx + 2f))

        composeRule.onNodeWithText("Mini controls").performTouchInput {
            swipe(center, Offset(center.x, center.y - 400f), 500L)
        }
        composeRule.waitUntil(5_000) { target == PlayerHostTarget.Expanded }
        assertEquals(1, createdViews)
        composeRule.runOnIdle {
            assertSame(player, firstView.player)
            assertTrue(player.currentPosition in miniPositionMs..(miniPositionMs + 10_000L))
        }
        val expandedVisualWidth = composeRule.onNodeWithTag(PLAYER_VIEWPORT_TAG)
            .fetchSemanticsNode().boundsInRoot.width
        assertTrue(expandedVisualWidth in (expandedWidthPx - 2f)..(expandedWidthPx + 2f))
        composeRule.runOnIdle { player.release() }
    }
}

private class HostSurfacePlayer(looper: Looper) : SimpleBasePlayer(looper) {
    override fun getState(): State = State.Builder()
        .setAvailableCommands(Player.Commands.Builder().addAllCommands().build())
        .setPlaybackState(Player.STATE_READY)
        .setPlayWhenReady(true, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
        .setPlaylist(
            listOf(
                MediaItemData.Builder("host-surface")
                    .setMediaItem(MediaItem.Builder().setMediaId("host-surface").build())
                    .build(),
            ),
        )
        .setContentPositionMs(42_000L)
        .build()

    override fun handleSetVideoOutput(videoOutput: Any): ListenableFuture<*> =
        Futures.immediateVoidFuture()

    override fun handleClearVideoOutput(videoOutput: Any?): ListenableFuture<*> =
        Futures.immediateVoidFuture()

    override fun handleRelease(): ListenableFuture<*> = Futures.immediateVoidFuture()
}
