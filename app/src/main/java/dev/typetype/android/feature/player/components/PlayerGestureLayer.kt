package dev.typetype.android.feature.player.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import dev.typetype.android.feature.player.state.DragMode
import dev.typetype.android.feature.player.state.GestureSide
import dev.typetype.android.feature.player.state.PlayerGestureState
import kotlin.math.abs

private const val BRIGHTNESS_DRAG_PIXELS_PER_FULL = 600f
private const val VOLUME_DRAG_PIXELS_PER_FULL = 600f
private const val DOUBLE_TAP_SEEK_INCREMENT_MS = 10_000L
private const val SEEK_DRAG_MS_PER_PIXEL = 80f
private const val DIRECTION_LOCK_THRESHOLD_PX = 8f
private const val LONG_PRESS_SPEED_FACTOR = 2f

data class PlayerGestureConfig(
    val doubleTapSeekEnabled: Boolean = true,
    val swipeSeekEnabled: Boolean = true,
    val swipeBrightnessVolumeEnabled: Boolean = true,
    val longPressSpeedEnabled: Boolean = true,
)

@Composable
fun PlayerGestureLayer(
    player: Player,
    state: PlayerGestureState,
    onTogglePlayPause: () -> Unit,
    onAdjustBrightness: (Float) -> Unit,
    onAdjustVolume: (Float) -> Unit,
    config: PlayerGestureConfig = PlayerGestureConfig(),
    modifier: Modifier = Modifier,
) {
    var savedSpeed = 1f
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(player, config) {
                detectTapGestures(
                    onTap = { onTogglePlayPause() },
                    onDoubleTap = { offset ->
                        if (!config.doubleTapSeekEnabled) return@detectTapGestures
                        val side = if (offset.x < size.width / 2f) GestureSide.Left else GestureSide.Right
                        val current = player.currentPosition
                        val target = if (side == GestureSide.Left) {
                            (current - DOUBLE_TAP_SEEK_INCREMENT_MS).coerceAtLeast(0L)
                        } else {
                            current + DOUBLE_TAP_SEEK_INCREMENT_MS
                        }
                        player.seekTo(target)
                        state.seekHintSide.value = side
                        state.seekHintSeconds.floatValue = (DOUBLE_TAP_SEEK_INCREMENT_MS / 1_000f)
                    },
                    onLongPress = {
                        if (!config.longPressSpeedEnabled) return@detectTapGestures
                        savedSpeed = player.playbackParameters.speed
                        player.setPlaybackSpeed(LONG_PRESS_SPEED_FACTOR)
                        state.longPressBoostActive.value = true
                    },
                    onPress = {
                        try {
                            awaitRelease()
                        } finally {
                            if (state.longPressBoostActive.value) {
                                player.setPlaybackSpeed(savedSpeed)
                                state.longPressBoostActive.value = false
                            }
                        }
                    },
                )
            }
            .pointerInput(player, config) {
                detectDragGestures(
                    onDragStart = {
                        state.dragMode.value = DragMode.None
                        state.seekDragStartMs.longValue = player.currentPosition
                        state.seekDragTargetMs.longValue = player.currentPosition
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (state.dragMode.value == DragMode.None) {
                            if (abs(dragAmount.x) < DIRECTION_LOCK_THRESHOLD_PX &&
                                abs(dragAmount.y) < DIRECTION_LOCK_THRESHOLD_PX
                            ) return@detectDragGestures
                            val candidate = pickDragMode(
                                dragAmount = dragAmount,
                                startX = change.position.x,
                                width = size.width.toFloat(),
                            )
                            val allowed = when (candidate) {
                                DragMode.Seek -> config.swipeSeekEnabled
                                DragMode.Brightness, DragMode.Volume -> config.swipeBrightnessVolumeEnabled
                                DragMode.None -> false
                            }
                            if (!allowed) return@detectDragGestures
                            state.dragMode.value = candidate
                            when (candidate) {
                                DragMode.Brightness -> state.brightnessOverlayActive.value = true
                                DragMode.Volume -> state.volumeOverlayActive.value = true
                                DragMode.Seek -> state.seekDragOverlayActive.value = true
                                DragMode.None -> Unit
                            }
                        }
                        when (state.dragMode.value) {
                            DragMode.Brightness -> {
                                val delta = -dragAmount.y / BRIGHTNESS_DRAG_PIXELS_PER_FULL
                                val next = (state.brightnessFraction.floatValue + delta).coerceIn(0f, 1f)
                                state.brightnessFraction.floatValue = next
                                onAdjustBrightness(next)
                            }
                            DragMode.Volume -> {
                                val delta = -dragAmount.y / VOLUME_DRAG_PIXELS_PER_FULL
                                val next = (state.volumeFraction.floatValue + delta).coerceIn(0f, 1f)
                                state.volumeFraction.floatValue = next
                                onAdjustVolume(next)
                            }
                            DragMode.Seek -> {
                                val deltaMs = (dragAmount.x * SEEK_DRAG_MS_PER_PIXEL).toLong()
                                val duration = if (player.duration > 0) player.duration else Long.MAX_VALUE
                                state.seekDragTargetMs.longValue =
                                    (state.seekDragTargetMs.longValue + deltaMs).coerceIn(0L, duration)
                            }
                            DragMode.None -> Unit
                        }
                    },
                    onDragEnd = {
                        if (state.dragMode.value == DragMode.Seek) {
                            player.seekTo(state.seekDragTargetMs.longValue)
                        }
                        resetDragState(state)
                    },
                    onDragCancel = { resetDragState(state) },
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (abs(zoom - 1f) > 0.05f) {
                        state.zoomFillMode.value = zoom > 1f
                    }
                }
            },
    ) {
        SeekHintOverlay(state = state)
        DragSliderOverlay(
            visible = state.brightnessOverlayActive.value,
            fraction = state.brightnessFraction.floatValue,
            label = "Brightness",
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp),
        )
        DragSliderOverlay(
            visible = state.volumeOverlayActive.value,
            fraction = state.volumeFraction.floatValue,
            label = "Volume",
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp),
        )
        SeekDragOverlay(state = state, durationMs = player.duration)
        SpeedBoostBadge(visible = state.longPressBoostActive.value, factor = LONG_PRESS_SPEED_FACTOR)
    }
}

private fun pickDragMode(dragAmount: Offset, startX: Float, width: Float): DragMode = when {
    abs(dragAmount.x) > abs(dragAmount.y) -> DragMode.Seek
    startX < width / 2f -> DragMode.Brightness
    else -> DragMode.Volume
}

private fun resetDragState(state: PlayerGestureState) {
    state.dragMode.value = DragMode.None
    state.brightnessOverlayActive.value = false
    state.volumeOverlayActive.value = false
    state.seekDragOverlayActive.value = false
}
