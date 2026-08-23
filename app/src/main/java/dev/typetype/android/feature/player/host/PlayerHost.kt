package dev.typetype.android.feature.player.host

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import dev.typetype.android.feature.player.PlayerFullscreenEffect
import dev.typetype.android.feature.player.PlayerRoute as PlayerRouteScreen
import dev.typetype.android.feature.player.components.rememberIsInPipMode
import dev.typetype.android.feature.player.components.rememberAccessiblePlayerControls

private val MINI_PLAYER_HEIGHT = 64.dp
internal const val PLAYER_HOST_OVERLAY_TAG = "player_host_overlay"

@Composable
fun PlayerHost(
    controller: PlayerHostController,
    bottomBarHeightDp: Float,
    isFullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    mediaController: MediaController?,
    onOpenChannel: (channelUrl: String) -> Unit,
    onOpenAccounts: () -> Unit,
    onClosePlayback: () -> Unit,
    accessibleControlsEnabled: Boolean = false,
    onTransitionProgressChange: (Float) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val state by controller.state.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val isInPip by rememberIsInPipMode()
    val accessibleControls = rememberAccessiblePlayerControls(accessibleControlsEnabled)
    val hapticFeedback = LocalHapticFeedback.current
    val activity = LocalActivity.current
    val configuration = LocalConfiguration.current
    val orientation = when (configuration.orientation) {
        Configuration.ORIENTATION_PORTRAIT -> DeviceOrientation.Portrait
        Configuration.ORIENTATION_LANDSCAPE -> DeviceOrientation.Landscape
        else -> DeviceOrientation.Other
    }
    var fullscreenOrientationState by rememberSaveable(
        stateSaver = FullscreenOrientationState.Saver,
    ) { mutableStateOf(FullscreenOrientationState()) }

    val navigationBarsBottom = WindowInsets.navigationBars.asPaddingValues()
        .calculateBottomPadding()
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val containerHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val miniHeightPx = with(density) { MINI_PLAYER_HEIGHT.toPx() }
        val bottomBarPx = with(density) { bottomBarHeightDp.dp.toPx() }
        val gestureBarPx = with(density) { navigationBarsBottom.toPx() }
        val miniAnchorPx = (
            containerHeightPx - miniHeightPx - bottomBarPx - gestureBarPx
        ).coerceAtLeast(0f)
        val hasFullscreenMedia = state.videoUrl != null &&
            state.target == PlayerHostTarget.Expanded &&
            !isInPip
        val allowsRotationFullscreen = hasFullscreenMedia && minOf(maxWidth, maxHeight) < 600.dp
        val requestFullscreen: (Boolean) -> Unit = { requested ->
            val transition = fullscreenOrientationState.onUserRequest(requested, orientation)
            fullscreenOrientationState = transition.state
            transition.fullscreenRequest?.let(onFullscreenChange)
        }

        PlayerFullscreenEffect(
            activity = activity,
            isFullscreen = isFullscreen,
            locksLandscape = fullscreenOrientationState.locksLandscape,
        )

        LaunchedEffect(orientation, hasFullscreenMedia, allowsRotationFullscreen, isFullscreen) {
            val transition = fullscreenOrientationState.onEnvironmentChanged(
                orientation = orientation,
                hasFullscreenMedia = hasFullscreenMedia,
                allowsRotationFullscreen = allowsRotationFullscreen,
                isFullscreen = isFullscreen,
            )
            fullscreenOrientationState = transition.state
            transition.fullscreenRequest?.let(onFullscreenChange)
        }

        LaunchedEffect(state.playbackClearRequestStamp, mediaController) {
            val requestStamp = state.playbackClearRequestStamp ?: return@LaunchedEffect
            val player = mediaController ?: return@LaunchedEffect
            player.stop()
            player.clearMediaItems()
            controller.acknowledgePlaybackClear(requestStamp)
        }
        LaunchedEffect(state.target, isFullscreen) {
            if (state.target != PlayerHostTarget.Expanded && isFullscreen) {
                requestFullscreen(false)
            }
        }
        LaunchedEffect(isInPip, isFullscreen) {
            if (isInPip && isFullscreen) {
                requestFullscreen(false)
            }
        }

        content()

        val hasVideo = state.videoUrl != null && state.target != PlayerHostTarget.Embedded

        if (hasVideo) {
            PlayerHostMotionLayout(
                target = state.target,
                requestStamp = state.requestStamp,
                miniAnchorPx = miniAnchorPx,
                containerHeightPx = containerHeightPx,
                miniHeightPx = miniHeightPx,
                dragEnabled = !isFullscreen && !isInPip && !accessibleControls,
                miniContentEnabled = !isInPip,
                onTargetSettled = { target ->
                    when (target) {
                        PlayerHostTarget.Expanded -> {
                            if (controller.state.value.target != target) controller.expand()
                        }
                        PlayerHostTarget.Mini -> {
                            if (controller.state.value.target != target) controller.minimize()
                        }
                        else -> Unit
                    }
                },
                onProgressChange = onTransitionProgressChange,
                onDragAnchorCrossed = {
                    hapticFeedback.performHapticFeedback(
                        HapticFeedbackType.GestureThresholdActivate,
                    )
                },
                onDragSettled = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                miniContent = {
                    MiniPlayerRuntime(
                        controller = mediaController,
                        onExpand = { controller.expand() },
                        onSendToBackground = { activity?.moveTaskToBack(false) },
                        onClose = onClosePlayback,
                    )
                },
                expandedContent = { transition ->
                    PlayerRouteScreen(
                        isFullscreen = isFullscreen,
                        hostTransitionProgress = transition.progress,
                        onFullscreenChange = requestFullscreen,
                        onNavigateBack = { controller.collapseExpanded() },
                        onOpenAccounts = {
                            controller.minimize()
                            onOpenAccounts()
                        },
                        onPlayVideo = { url -> controller.openVideo(url) },
                        onAutoplayVideo = { url -> controller.continueWithVideo(url) },
                        onOpenChannel = { url ->
                            controller.minimize()
                            onOpenChannel(url)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                },
            )

            BackHandler(enabled = isFullscreen || state.target == PlayerHostTarget.Expanded) {
                if (isFullscreen) {
                    requestFullscreen(false)
                } else {
                    controller.collapseExpanded()
                }
            }
        } else {
            LaunchedEffect(Unit) { onTransitionProgressChange(0f) }
        }
    }
}
