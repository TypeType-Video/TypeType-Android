package dev.typetype.android.feature.player.host

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import dev.typetype.android.feature.player.FullscreenTestActivity
import dev.typetype.android.feature.player.PlayerContentLayout
import dev.typetype.android.feature.player.PlayerFullscreenEffect
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

    @Before
    fun forcePortraitAndShowPlayer() {
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
                    dragEnabled = true,
                    miniContentEnabled = true,
                    fullscreenCenterDragEnabled = isFullscreen,
                    onTargetSettled = { settledTarget ->
                        target = settledTarget
                        if (settledTarget == PlayerHostTarget.Mini) isFullscreen = false
                        requestStamp += 1
                    },
                    onProgressChange = {},
                    miniContent = { Box(Modifier.testTag(MINI_CONTENT_TAG)) },
                    expandedContent = { transition ->
                        PlayerContentLayout(
                            isFullscreen = isFullscreen,
                            hostTransitionProgress = transition.progress,
                            viewport = { Box(it.testTag(VIEWPORT_TAG)) },
                            details = { Box(it.height(240.dp)) },
                        )
                    },
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun swipeDownExitsFullscreenIntoAPortraitMiniPlayer() {
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.activity.resources.configuration.orientation ==
                Configuration.ORIENTATION_LANDSCAPE
        }

        composeRule.onNodeWithTag(PLAYER_HOST_OVERLAY_TAG).performTouchInput {
            swipe(center, center.copy(y = bottom - 1f), 700L)
        }

        composeRule.waitUntil(TIMEOUT_MILLIS) {
            target == PlayerHostTarget.Mini && !isFullscreen
        }
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.activity.resources.configuration.orientation ==
                Configuration.ORIENTATION_PORTRAIT
        }

        val hostBounds = composeRule.onNodeWithTag(PLAYER_HOST_OVERLAY_TAG)
            .fetchSemanticsNode()
            .boundsInRoot
        val expectedMiniHeightPx = with(composeRule.density) { MINI_HEIGHT.toPx() }
        assertEquals(expectedMiniHeightPx, hostBounds.height, 1f)
    }

    private companion object {
        val MINI_HEIGHT = 64.dp
        const val MINI_CONTENT_TAG = "player_fullscreen_gesture_mini"
        const val VIEWPORT_TAG = "player_fullscreen_gesture_viewport"
        const val TIMEOUT_MILLIS = 15_000L
    }
}
