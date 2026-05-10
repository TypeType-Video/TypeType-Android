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
import androidx.compose.foundation.layout.statusBars
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
import dev.typetype.android.feature.player.components.rememberIsInPipMode
import kotlin.math.abs
import kotlinx.coroutines.launch

private val MINI_PLAYER_HEIGHT = 64.dp

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
    val isInPip by rememberIsInPipMode()

    val navigationBarsBottom = WindowInsets.navigationBars.asPaddingValues()
        .calculateBottomPadding()
    val statusBarsTop = WindowInsets.statusBars.asPaddingValues()
        .calculateTopPadding()
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

        LaunchedEffect(anchoredState.settledValue) {
            controller.onAnchorSettled(anchoredState.settledValue)
        }

        content()

        val hasVideo = state.videoUrl != null ||
            anchoredState.currentValue != PlayerHostTarget.Hidden ||
            anchoredState.targetValue != PlayerHostTarget.Hidden

        if (hasVideo) {
            val isMini = !isInPip && (
                anchoredState.currentValue == PlayerHostTarget.Mini ||
                    anchoredState.targetValue == PlayerHostTarget.Mini
                )

            val rawOffsetPx = if (anchoredState.anchors.size > 0) {
                anchoredState.requireOffset()
            } else {
                containerHeightPx
            }
            val miniProgress = if (miniAnchorPx > 0f) {
                (rawOffsetPx / miniAnchorPx).coerceIn(0f, 1f)
            } else {
                0f
            }
            val effectiveHeightPx = (
                containerHeightPx + (miniHeightPx - containerHeightPx) * miniProgress
                ).coerceAtLeast(miniHeightPx)
            val effectiveHeightDp = with(density) { effectiveHeightPx.toDp() }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(effectiveHeightDp)
                    .offset {
                        IntOffset(0, rawOffsetPx.toInt())
                    }
                    .pointerInput(anchoredState) {
                        val playerAreaHeightPx = with(density) {
                            statusBarsTop.toPx()
                        } + (size.width.toFloat() * 9f / 16f)

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
                                    val isClearVertical = absDy > absDx * 1.5f && absDy > 60f
                                    val current = anchoredState.currentValue
                                    val triggered = isClearVertical && when (current) {
                                        PlayerHostTarget.Expanded ->
                                            movement.y > 60f && startPos.y < playerAreaHeightPx
                                        PlayerHostTarget.Mini -> false
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
                        onOpenChannel = { url ->
                            controller.minimize()
                            onOpenChannel(url)
                        },
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
