package dev.typetype.android.feature.player.host

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import dev.typetype.android.feature.player.PlayerRoute as PlayerRouteScreen
import dev.typetype.android.feature.player.components.MiniPlayerBar
import kotlin.math.abs
import kotlinx.coroutines.launch

private val MINI_PLAYER_HEIGHT = 64.dp

/**
 * Floating player overlay that slides between Expanded (full screen), Mini
 * (compact bar above the bottom bar), and Hidden (fully off-screen) states.
 *
 * Replaces the old PlayerRoute navigation entry. Inspired by:
 * - LibreTube `SingleViewTouchableMotionLayout` + `player_scene.xml`
 *   (drag-down via MotionLayout `OnSwipe motion:dragDirection="dragDown"`)
 * - PipePipe `setupBottomPlayer` with `BottomSheetBehavior` and three states
 *   (HIDDEN / COLLAPSED with peekHeight / EXPANDED) — see VideoDetailFragment.java:2500.
 *
 * The host owns the AnchoredDraggable; the [PlayerHostController] only signals
 * "open this video / minimize / hide" — all gesture-driven transitions are
 * handled here and reported back via [PlayerHostController.onAnchorSettled].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerHost(
    controller: PlayerHostController,
    bottomBarHeightDp: Float,
    mediaController: MediaController?,
    onOpenChannel: (channelUrl: String) -> Unit,
    content: @Composable () -> Unit,
) {
    val state by controller.state.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

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

        val anchors = remember(containerHeightPx, miniAnchorPx) {
            DraggableAnchors {
                PlayerHostTarget.Expanded at 0f
                PlayerHostTarget.Mini at miniAnchorPx
                PlayerHostTarget.Hidden at containerHeightPx
            }
        }

        val anchoredState = remember {
            AnchoredDraggableState(
                initialValue = PlayerHostTarget.Hidden,
                positionalThreshold = { distance -> distance * 0.5f },
                velocityThreshold = { with(density) { 100.dp.toPx() } },
                snapAnimationSpec = tween(durationMillis = 280),
                decayAnimationSpec = exponentialDecay(),
            )
        }

        LaunchedEffect(anchors) {
            anchoredState.updateAnchors(anchors)
        }

        // React to controller state changes (openVideo / hide / minimize)
        LaunchedEffect(state.requestStamp) {
            val target = state.target
            if (anchoredState.currentValue != target) {
                anchoredState.animateTo(target)
            }
            if (target == PlayerHostTarget.Hidden) {
                mediaController?.let { ctrl ->
                    ctrl.stop()
                    ctrl.clearMediaItems()
                }
            }
        }

        // Report user-driven settling back to the controller so other parts of
        // the app see the new target (e.g. a deep-link openVideo while we're
        // mid-drag).
        LaunchedEffect(anchoredState.settledValue) {
            controller.onAnchorSettled(anchoredState.settledValue)
        }

        // Background content (the NavHost) is always rendered.
        content()

        val hasVideo = state.videoUrl != null ||
            anchoredState.currentValue != PlayerHostTarget.Hidden ||
            anchoredState.targetValue != PlayerHostTarget.Hidden

        if (hasVideo) {
            // Collapse-to-mini swipe handler for the player area.
            val isMini = anchoredState.currentValue == PlayerHostTarget.Mini ||
                anchoredState.targetValue == PlayerHostTarget.Mini
            val isExpanded = anchoredState.currentValue == PlayerHostTarget.Expanded &&
                abs(anchoredState.requireOffset()) < 1f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        val raw = if (anchoredState.anchors.size > 0) {
                            anchoredState.requireOffset()
                        } else {
                            containerHeightPx
                        }
                        IntOffset(0, raw.toInt())
                    }
                    .pointerInput(anchoredState) {
                        awaitEachGesture {
                            val down = awaitFirstDown(
                                requireUnconsumed = false,
                                pass = PointerEventPass.Initial,
                            )
                            val startPos = down.position
                            val tracker = VelocityTracker()
                            tracker.addPosition(down.uptimeMillis, startPos)
                            var intercepting = false

                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull { it.id == down.id }
                                    ?: break
                                if (!change.pressed) {
                                    if (intercepting) {
                                        change.consume()
                                        val velocity = tracker.calculateVelocity().y
                                        coroutineScope.launch { anchoredState.settle(velocity) }
                                    }
                                    break
                                }
                                tracker.addPosition(change.uptimeMillis, change.position)
                                val movement = change.position - startPos

                                if (!intercepting) {
                                    val absDy = abs(movement.y)
                                    val absDx = abs(movement.x)
                                    val isClearVertical = absDy > absDx * 1.5f && absDy > 28f
                                    val triggered = isClearVertical && when (anchoredState.currentValue) {
                                        PlayerHostTarget.Expanded -> movement.y > 28f
                                        PlayerHostTarget.Mini -> true
                                        PlayerHostTarget.Hidden -> false
                                    }
                                    if (triggered) {
                                        intercepting = true
                                        change.consume()
                                        anchoredState.dispatchRawDelta(movement.y)
                                    }
                                } else {
                                    val delta = change.positionChange().y
                                    if (delta != 0f) {
                                        change.consume()
                                        anchoredState.dispatchRawDelta(delta)
                                    }
                                }
                            }
                        }
                    }
                    .background(if (isMini) Color.Transparent else Color.Black),
            ) {
                if (isMini) {
                    MiniSlot(
                        controller = mediaController,
                        onExpand = { controller.expand() },
                        onClose = { controller.hide() },
                    )
                } else {
                    PlayerRouteScreen(
                        onNavigateBack = { controller.minimize() },
                        onPlayVideo = { url -> controller.openVideo(url) },
                        onOpenChannel = onOpenChannel,
                    )
                }
            }

            BackHandler(enabled = state.target != PlayerHostTarget.Hidden) {
                when (anchoredState.currentValue) {
                    PlayerHostTarget.Expanded -> controller.minimize()
                    PlayerHostTarget.Mini -> controller.hide()
                    PlayerHostTarget.Hidden -> Unit
                }
            }
        }
    }
}

@Composable
private fun MiniSlot(
    controller: MediaController?,
    onExpand: () -> Unit,
    onClose: () -> Unit,
) {
    val item = controller?.currentMediaItem
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MINI_PLAYER_HEIGHT),
        contentAlignment = Alignment.TopStart,
    ) {
        if (controller != null && item != null) {
            MiniPlayerBar(
                player = controller,
                title = item.mediaMetadata.title?.toString().orEmpty(),
                subtitle = item.mediaMetadata.artist?.toString().orEmpty(),
                artworkUri = item.mediaMetadata.artworkUri?.toString(),
                onExpand = onExpand,
                onClose = onClose,
            )
        }
    }
}
