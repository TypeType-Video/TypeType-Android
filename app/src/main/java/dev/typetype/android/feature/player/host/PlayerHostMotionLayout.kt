package dev.typetype.android.feature.player.host

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

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
    onDragAnchorCrossed: () -> Unit = {},
    onDragSettled: () -> Unit = {},
    miniContent: @Composable () -> Unit,
    expandedContent: @Composable (PlayerHostTransition) -> Unit,
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
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    var dragSettlementPending by remember { mutableStateOf(false) }
    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(anchoredState)
    val nestedScrollConnection = remember(anchoredState, dragEnabled, flingBehavior) {
        playerHostNestedScrollConnection(anchoredState, flingBehavior, dragEnabled)
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
    LaunchedEffect(anchoredState, isDragged) {
        if (!isDragged) return@LaunchedEffect
        snapshotFlow { anchoredState.targetValue }
            .distinctUntilChanged()
            .drop(1)
            .collect { onDragAnchorCrossed() }
    }
    LaunchedEffect(anchoredState) {
        snapshotFlow {
            isDragged to (
                !anchoredState.isAnimationRunning &&
                    anchoredState.currentValue == anchoredState.settledValue &&
                    anchoredState.targetValue == anchoredState.settledValue
                )
        }.distinctUntilChanged().collect { (dragged, settled) ->
            if (dragged) {
                dragSettlementPending = true
            } else if (dragSettlementPending && settled) {
                dragSettlementPending = false
                onDragSettled()
            }
        }
    }

    val transition = playerHostTransition(
        offsetPx = anchoredState.offset,
        miniAnchorPx = miniAnchorPx,
        containerHeightPx = containerHeightPx,
        miniHeightPx = miniHeightPx,
        isAnimationRunning = anchoredState.isAnimationRunning,
    )
    val height = with(density) { transition.heightPx.toDp() }
    SideEffect { onProgressChange(transition.progress) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .testTag(PLAYER_HOST_OVERLAY_TAG)
            .offset { IntOffset(0, transition.offsetPx) }
            .nestedScroll(nestedScrollConnection)
            .anchoredDraggable(
                state = anchoredState,
                orientation = Orientation.Vertical,
                enabled = dragEnabled,
                interactionSource = interactionSource,
                flingBehavior = flingBehavior,
            )
            .background(lerp(Color.Black, MaterialTheme.colorScheme.surface, transition.progress)),
    ) {
        expandedContent(transition)
        if (miniContentEnabled && transition.progress > 0.001f) {
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = transition.miniContentAlpha
                },
            ) {
                miniContent()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun playerHostNestedScrollConnection(
    state: AnchoredDraggableState<PlayerHostTarget>,
    flingBehavior: TargetedFlingBehavior,
    enabled: Boolean,
): NestedScrollConnection = object : NestedScrollConnection {
    private val scrollScope = object : ScrollScope {
        override fun scrollBy(pixels: Float): Float = state.dispatchRawDelta(pixels)
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val offset = state.offset.takeIf(Float::isFinite) ?: 0f
        if (!enabled || offset <= 0f || available.y == 0f) return Offset.Zero
        return Offset(0f, state.dispatchRawDelta(available.y))
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (!enabled || available.y <= 0f) return Offset.Zero
        return Offset(0f, state.dispatchRawDelta(available.y))
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val offset = state.offset.takeIf(Float::isFinite) ?: 0f
        if (!enabled || offset <= 0f) return Velocity.Zero
        val remaining = with(flingBehavior) { scrollScope.performFling(available.y) }
        state.animateTo(requireNotNull(state.anchors.closestAnchor(state.requireOffset())))
        return Velocity(0f, available.y - remaining)
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val offset = state.offset.takeIf(Float::isFinite) ?: 0f
        if (!enabled || offset <= 0f) return Velocity.Zero
        val velocity = available.y.takeIf { it != 0f } ?: consumed.y
        with(flingBehavior) { scrollScope.performFling(velocity) }
        state.animateTo(requireNotNull(state.anchors.closestAnchor(state.requireOffset())))
        return Velocity(0f, available.y)
    }
}

private fun PlayerHostTarget.draggableTarget(): PlayerHostTarget = when (this) {
    PlayerHostTarget.Embedded, PlayerHostTarget.Hidden -> PlayerHostTarget.Expanded
    else -> this
}
