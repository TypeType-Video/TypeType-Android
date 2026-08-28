package dev.typetype.android.feature.player.host

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.typetype.android.feature.player.PLAYER_VIEWPORT_TAG
import dev.typetype.android.feature.player.FullscreenTestActivity
import dev.typetype.android.feature.player.PlayerContentLayout
import dev.typetype.android.feature.player.PlayerFullscreenEffect
import dev.typetype.android.feature.player.components.PlayerGestureLayer
import dev.typetype.android.feature.player.state.PlayerGestureState
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PlayerFullscreenGestureIntegrationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<FullscreenTestActivity>()

    private var target by mutableStateOf(PlayerHostTarget.Expanded)
    private var isFullscreen by mutableStateOf(true)
    private var requestStamp by mutableLongStateOf(0L)
    private lateinit var player: FullscreenGesturePlayer

    @Before
    fun forcePortraitAndShowPlayer() {
        player = FullscreenGesturePlayer(Looper.getMainLooper())
        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.activity.resources.configuration.orientation ==
                Configuration.ORIENTATION_PORTRAIT
        }
        composeRule.setContent {
            PlayerFullscreenEffect(
                activity = composeRule.activity,
                isFullscreen = isFullscreen,
                locksLandscape = true,
                restoresPortraitOnExit = true,
            )
            val density = LocalDensity.current
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val containerHeightPx = constraints.maxHeight.toFloat()
                val miniHeightPx = with(density) { MINI_HEIGHT.toPx() }
                PlayerHostMotionLayout(
                    target = target,
                    requestStamp = requestStamp,
                    miniAnchorPx = containerHeightPx - miniHeightPx,
                    containerHeightPx = containerHeightPx,
                    miniHeightPx = miniHeightPx,
                    dragEnabled = !isFullscreen,
                    miniContentEnabled = true,
                    onTargetSettled = { settledTarget ->
                        target = settledTarget
                        requestStamp += 1
                    },
                    onProgressChange = {},
                    miniContent = { Box(Modifier.testTag(MINI_CONTENT_TAG)) },
                    expandedContent = { transitionProgress ->
                        PlayerContentLayout(
                            isFullscreen = isFullscreen,
                            hostTransitionProgress = transitionProgress,
                            viewport = { viewportModifier ->
                                Box(viewportModifier) {
                                    PlayerGestureLayer(
                                        player = player,
                                        state = remember { PlayerGestureState() },
                                        onSingleTap = {},
                                        onAdjustBrightness = {},
                                        onAdjustVolume = {},
                                        isFullscreen = isFullscreen,
                                        onExitFullscreenGesture = { isFullscreen = false },
                                        modifier = Modifier.fillMaxSize().testTag(GESTURE_TAG),
                                    )
                                }
                            },
                            details = { Box(it.height(240.dp).testTag(DETAILS_TAG)) },
                        )
                    },
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun swipeDownExitsFullscreenWithoutMinimizingThePlayer() {
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.activity.resources.configuration.orientation ==
                Configuration.ORIENTATION_LANDSCAPE
        }

        composeRule.onNodeWithTag(GESTURE_TAG).performTouchInput {
            swipe(center, center.copy(y = bottom - 1f), 700L)
        }

        composeRule.waitUntil(TIMEOUT_MILLIS) { !isFullscreen }
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.activity.resources.configuration.orientation ==
                Configuration.ORIENTATION_PORTRAIT
        }

        val hostBounds = composeRule.onNodeWithTag(PLAYER_HOST_OVERLAY_TAG)
            .fetchSemanticsNode()
            .boundsInRoot
        val miniHeightPx = with(composeRule.density) { MINI_HEIGHT.toPx() }
        assertEquals(PlayerHostTarget.Expanded, target)
        org.junit.Assert.assertTrue(hostBounds.height > miniHeightPx * 2f)
        val viewportBounds = composeRule.onNodeWithTag(PLAYER_VIEWPORT_TAG)
            .fetchSemanticsNode()
            .boundsInRoot
        val detailsBounds = composeRule.onNodeWithTag(DETAILS_TAG)
            .fetchSemanticsNode()
            .boundsInRoot
        assertEquals(16f / 9f, viewportBounds.width / viewportBounds.height, 0.01f)
        assertEquals(viewportBounds.bottom, detailsBounds.top, 1f)
        composeRule.runOnIdle { player.release() }
    }

    private companion object {
        val MINI_HEIGHT = 64.dp
        const val MINI_CONTENT_TAG = "player_fullscreen_gesture_mini"
        const val GESTURE_TAG = "player_fullscreen_gesture_layer"
        const val DETAILS_TAG = "player_fullscreen_gesture_details"
        const val TIMEOUT_MILLIS = 15_000L
    }
}

private class FullscreenGesturePlayer(looper: Looper) : SimpleBasePlayer(looper) {
    override fun getState(): State = State.Builder()
        .setAvailableCommands(Player.Commands.Builder().addAllCommands().build())
        .setPlaybackState(Player.STATE_READY)
        .setPlaylist(
            listOf(
                MediaItemData.Builder("fullscreen-gesture")
                    .setMediaItem(MediaItem.Builder().setMediaId("fullscreen-gesture").build())
                    .build(),
            ),
        )
        .build()

    override fun handleRelease(): ListenableFuture<*> = Futures.immediateVoidFuture()
}
