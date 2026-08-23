package dev.typetype.android.feature.player.host

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.drag
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

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
    fullscreenCenterDragEnabled: Boolean = false,
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
    val centerDragScope = rememberCoroutineScope()
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
            .fullscreenCenterDrag(
                state = anchoredState,
                scope = centerDragScope,
                flingBehavior = flingBehavior,
                enabled = dragEnabled && fullscreenCenterDragEnabled,
                onAnchorCrossed = onDragAnchorCrossed,
                onSettled = onDragSettled,
            )
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
private fun Modifier.fullscreenCenterDrag(
    state: AnchoredDraggableState<PlayerHostTarget>,
    scope: CoroutineScope,
    flingBehavior: TargetedFlingBehavior,
    enabled: Boolean,
    onAnchorCrossed: () -> Unit,
    onSettled: () -> Unit,
): Modifier = if (!enabled) {
    this
} else {
    pointerInput(state) {
        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Initial,
            )
            if (down.position.x !in (size.width * 0.35f)..(size.width * 0.65f)) {
                return@awaitEachGesture
            }
            val tracker = VelocityTracker()
            tracker.addPosition(down.uptimeMillis, down.position)
            var lastPosition = down.position
            var totalX = 0f
            var totalY = 0f
            var dragging = false
            var completed = false
            val initialTarget = state.settledValue
            var lastTarget = state.targetValue
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                tracker.addPosition(change.uptimeMillis, change.position)
                if (!change.pressed) break
                val delta = change.position - lastPosition
                lastPosition = change.position
                totalX += delta.x
                totalY += delta.y
                if (!dragging) {
                    if (kotlin.math.abs(totalX) < viewConfiguration.touchSlop &&
                        kotlin.math.abs(totalY) < viewConfiguration.touchSlop
                    ) continue
                    if (totalY <= kotlin.math.abs(totalX)) break
                    dragging = true
                }
                state.dispatchRawDelta(delta.y)
                change.consume()
                if (state.targetValue != lastTarget) {
                    lastTarget = state.targetValue
                    onAnchorCrossed()
                }
                completed = drag(down.id) { dragChange ->
                    tracker.addPosition(dragChange.uptimeMillis, dragChange.position)
                    val dragDelta = dragChange.position - lastPosition
                    lastPosition = dragChange.position
                    state.dispatchRawDelta(dragDelta.y)
                    dragChange.consume()
                    if (state.targetValue != lastTarget) {
                        lastTarget = state.targetValue
                        onAnchorCrossed()
                    }
                }
                break
            }
            if (dragging) {
                val velocity = tracker.calculateVelocity().y
                scope.launch {
                    if (!completed) {
                        state.animateTo(initialTarget)
                        return@launch
                    }
                    val scrollScope = object : ScrollScope {
                        override fun scrollBy(pixels: Float): Float =
                            state.dispatchRawDelta(pixels)
                    }
                    with(flingBehavior) { scrollScope.performFling(velocity) }
                    state.animateTo(
                        requireNotNull(state.anchors.closestAnchor(state.requireOffset())),
                    )
                    onSettled()
                }
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
