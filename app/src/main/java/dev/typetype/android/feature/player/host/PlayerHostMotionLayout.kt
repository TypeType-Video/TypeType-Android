package dev.typetype.android.feature.player.host

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PlayerHostMotionLayout(
    target: PlayerHostTarget,
    requestStamp: Long,
    miniAnchorPx: Float,
    containerHeightPx: Float,
    miniHeightPx: Float,
    dragEnabled: Boolean,
    miniContentEnabled: Boolean,
    onTargetSettled: (PlayerHostTarget) -> Unit,
    onProgressChange: (Float) -> Unit,
    miniContent: @Composable () -> Unit,
    expandedContent: @Composable (Modifier) -> Unit,
) {
    val density = LocalDensity.current
    val anchors = remember(miniAnchorPx) {
        DraggableAnchors {
            PlayerHostTarget.Expanded at 0f
            PlayerHostTarget.Mini at miniAnchorPx
        }
    }
    val anchoredState = remember {
        AnchoredDraggableState(initialValue = target.draggableTarget())
    }
    LaunchedEffect(anchors, requestStamp) {
        val requestedTarget = target.draggableTarget()
        anchoredState.updateAnchors(anchors, requestedTarget)
        if (anchoredState.currentValue != requestedTarget) {
            anchoredState.animateTo(requestedTarget)
        }
    }
    LaunchedEffect(anchoredState) {
        snapshotFlow { anchoredState.settledValue }
            .distinctUntilChanged()
            .collect(onTargetSettled)
    }

    val transition = playerHostTransition(
        offsetPx = anchoredState.offset,
        miniAnchorPx = miniAnchorPx,
        containerHeightPx = containerHeightPx,
        miniHeightPx = miniHeightPx,
        isAnimationRunning = anchoredState.isAnimationRunning,
    )
    val isMini = miniContentEnabled && transition.isSettledMini
    val height = with(density) { transition.heightPx.toDp() }
    SideEffect { onProgressChange(transition.progress) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .testTag(PLAYER_HOST_OVERLAY_TAG)
            .offset { IntOffset(0, transition.offsetPx) }
            .anchoredDraggable(
                state = anchoredState,
                orientation = Orientation.Vertical,
                enabled = dragEnabled,
            )
            .background(
                if (isMini) Color.Transparent
                else Color.Black.copy(alpha = 1f - transition.progress),
            ),
    ) {
        if (isMini) {
            miniContent()
        } else {
            expandedContent(
                Modifier.graphicsLayer {
                    alpha = transition.expandedContentAlpha
                    clip = true
                },
            )
        }
    }
}

private fun PlayerHostTarget.draggableTarget(): PlayerHostTarget = when (this) {
    PlayerHostTarget.Embedded, PlayerHostTarget.Hidden -> PlayerHostTarget.Expanded
    else -> this
}
