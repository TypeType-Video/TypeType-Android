package dev.typetype.android.feature.player.components

import androidx.compose.ui.geometry.Offset
import dev.typetype.android.feature.player.state.DragMode
import dev.typetype.android.feature.player.state.PlayerGestureState
import kotlin.math.abs

private const val LEVEL_DRAG_VIEW_FRACTION = 0.75f

internal fun handleDragMode(
    state: PlayerGestureState,
    mode: DragMode,
    delta: Offset,
    levelDragRangePx: Float,
    onAdjustBrightness: (Float) -> Unit,
    onAdjustVolume: (Float) -> Unit,
) {
    when (mode) {
        DragMode.Brightness -> {
            val next = adjustLevelFraction(
                state.brightnessFraction.floatValue,
                delta.y,
                levelDragRangePx,
            )
            state.brightnessFraction.floatValue = next
            onAdjustBrightness(next)
        }
        DragMode.Volume -> {
            val next = adjustLevelFraction(
                state.volumeFraction.floatValue,
                delta.y,
                levelDragRangePx,
            )
            state.volumeFraction.floatValue = next
            onAdjustVolume(next)
        }
        DragMode.Seek,
        DragMode.FullscreenEnter,
        DragMode.FullscreenExit,
        DragMode.None,
        -> Unit
    }
}

internal fun adjustLevelFraction(current: Float, deltaY: Float, dragRangePx: Float): Float =
    (current - deltaY / dragRangePx.coerceAtLeast(1f)).coerceIn(0f, 1f)

internal fun levelDragRangePx(width: Float, height: Float): Float =
    minOf(width, height) * LEVEL_DRAG_VIEW_FRACTION

internal fun pickDragMode(dragAmount: Offset, startX: Float, width: Float): DragMode = when {
    abs(dragAmount.x) > abs(dragAmount.y) * 1.5f -> DragMode.Seek
    abs(dragAmount.x) > abs(dragAmount.y) -> DragMode.None
    startX in (width * 0.35f)..(width * 0.65f) && dragAmount.y < 0f -> DragMode.FullscreenEnter
    startX in (width * 0.35f)..(width * 0.65f) && dragAmount.y > 0f -> DragMode.FullscreenExit
    startX < width / 2f -> DragMode.Brightness
    startX > width * 2f / 3f -> DragMode.Volume
    else -> DragMode.None
}
