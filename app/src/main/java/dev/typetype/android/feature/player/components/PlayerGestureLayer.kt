package dev.typetype.android.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import dev.typetype.android.feature.player.state.GestureSide
import dev.typetype.android.feature.player.state.PlayerGestureState
import kotlinx.coroutines.delay
import kotlin.math.abs

private const val SEEK_HINT_VISIBLE_MS = 600L
private const val DRAG_OVERLAY_VISIBLE_MS = 800L
private const val BRIGHTNESS_DRAG_PIXELS_PER_FULL = 600f
private const val VOLUME_DRAG_PIXELS_PER_FULL = 600f
private const val DOUBLE_TAP_SEEK_INCREMENT_MS = 10_000L

@Composable
fun PlayerGestureLayer(
    player: Player,
    state: PlayerGestureState,
    onTogglePlayPause: () -> Unit,
    onAdjustBrightness: (Float) -> Unit,
    onAdjustVolume: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(player) {
                detectTapGestures(
                    onTap = { onTogglePlayPause() },
                    onDoubleTap = { offset ->
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
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        if (offset.x < size.width / 2f) {
                            state.brightnessOverlayActive.value = true
                        } else {
                            state.volumeOverlayActive.value = true
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        if (state.brightnessOverlayActive.value) {
                            val delta = -dragAmount / BRIGHTNESS_DRAG_PIXELS_PER_FULL
                            val next = (state.brightnessFraction.floatValue + delta)
                                .coerceIn(0f, 1f)
                            state.brightnessFraction.floatValue = next
                            onAdjustBrightness(next)
                        } else if (state.volumeOverlayActive.value) {
                            val delta = -dragAmount / VOLUME_DRAG_PIXELS_PER_FULL
                            val next = (state.volumeFraction.floatValue + delta)
                                .coerceIn(0f, 1f)
                            state.volumeFraction.floatValue = next
                            onAdjustVolume(next)
                        }
                    },
                    onDragEnd = {
                        state.brightnessOverlayActive.value = false
                        state.volumeOverlayActive.value = false
                    },
                    onDragCancel = {
                        state.brightnessOverlayActive.value = false
                        state.volumeOverlayActive.value = false
                    },
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
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp),
        )
        DragSliderOverlay(
            visible = state.volumeOverlayActive.value,
            fraction = state.volumeFraction.floatValue,
            label = "Volume",
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp),
        )
    }
}

@Composable
private fun SeekHintOverlay(state: PlayerGestureState) {
    val side = state.seekHintSide.value
    LaunchedEffect(side, state.seekHintSeconds.floatValue) {
        if (side != null) {
            delay(SEEK_HINT_VISIBLE_MS)
            state.seekHintSide.value = null
        }
    }
    if (side != null) {
        Box(modifier = Modifier.fillMaxSize()) {
            val alignment = if (side == GestureSide.Left) Alignment.CenterStart else Alignment.CenterEnd
            val sign = if (side == GestureSide.Left) "-" else "+"
            Box(
                modifier = Modifier
                    .align(alignment)
                    .padding(horizontal = 48.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "$sign${state.seekHintSeconds.floatValue.toInt()}s",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun DragSliderOverlay(
    visible: Boolean,
    fraction: Float,
    label: String,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(180.dp)
            .width(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "${(fraction * 100).toInt()}%",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
        )
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxHeight(0.7f)
                .width(4.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.White.copy(alpha = 0.3f),
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
