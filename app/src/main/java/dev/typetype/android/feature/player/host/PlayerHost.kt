package dev.typetype.android.feature.player.host

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateTo
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import dev.typetype.android.feature.player.PlayerRoute as PlayerRouteScreen
import dev.typetype.android.feature.player.components.MiniPlayerBar
import dev.typetype.android.feature.player.components.rememberIsInPipMode

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
    val isInPip by rememberIsInPipMode()
    val activity = LocalActivity.current

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
                anchoredState.currentValue == PlayerHostTarget.Mini &&
                    anchoredState.targetValue == PlayerHostTarget.Mini
                )

            val rawOffsetPx = if (anchoredState.anchors.size > 0) {
                anchoredState.requireOffset()
            } else {
                containerHeightPx
            }
            val hostHeightDp = with(density) {
                if (isMini) miniHeightPx.toDp() else containerHeightPx.toDp()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(hostHeightDp)
                    .offset {
                        IntOffset(0, rawOffsetPx.toInt())
                    }
                    .background(if (isMini) Color.Transparent else Color.Black),
            ) {
                if (isMini) {
                    MiniSlot(
                        controller = mediaController,
                        onExpand = { controller.expand() },
                        onSendToBackground = { activity?.moveTaskToBack(false) },
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
    onSendToBackground: () -> Unit,
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
                onSendToBackground = onSendToBackground,
                onClose = onClose,
            )
        }
    }
}
