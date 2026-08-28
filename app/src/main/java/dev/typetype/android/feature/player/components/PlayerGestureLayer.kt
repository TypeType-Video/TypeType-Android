package dev.typetype.android.feature.player.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.media3.common.Player
import dev.typetype.android.R
import dev.typetype.android.feature.player.state.DragMode
import dev.typetype.android.feature.player.state.GestureSide
import dev.typetype.android.feature.player.state.PlayerGestureState
import dev.typetype.android.feature.player.state.ResizeMode
import kotlin.math.abs

private const val SEEK_DRAG_MS_PER_PIXEL = 80f
private const val DIRECTION_LOCK_THRESHOLD_PX = 18f
private const val LONG_PRESS_SPEED_FACTOR = 2f

data class PlayerGestureConfig(
    val doubleTapSeekEnabled: Boolean = true,
    val doubleTapSeekSeconds: Int = 10,
    val swipeSeekEnabled: Boolean = false,
    val swipeBrightnessVolumeEnabled: Boolean = true,
    val longPressSpeedEnabled: Boolean = true,
    val accessibleControlsEnabled: Boolean = false,
)

@Composable
fun PlayerGestureLayer(
    player: Player,
    state: PlayerGestureState,
    onSingleTap: () -> Unit,
    onAdjustBrightness: (Float) -> Unit,
    onAdjustVolume: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onGestureFeedback: () -> Unit = {},
    isFullscreen: Boolean = false,
    onEnterFullscreenGesture: () -> Unit = {},
    onExitFullscreenGesture: () -> Unit = {},
    fullscreenExitGestureEnabled: Boolean = true,
    config: PlayerGestureConfig = PlayerGestureConfig(),
) {
    var savedSpeed by remember { mutableFloatStateOf(1f) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(player, config, isFullscreen) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial,
                    )
                    val startX = down.position.x
                    var lastPosition = down.position
                    var totalDrag = Offset.Zero
                    var mode = DragMode.None
                    state.dragMode.value = DragMode.None
                    state.seekDragStartMs.longValue = player.currentPosition
                    state.seekDragTargetMs.longValue = player.currentPosition

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.count { it.pressed } > 1) {
                            resetDragState(state)
                            break
                        }
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            when (mode) {
                                DragMode.Seek -> player.seekTo(state.seekDragTargetMs.longValue)
                                DragMode.FullscreenEnter -> onEnterFullscreenGesture()
                                DragMode.FullscreenExit -> onExitFullscreenGesture()
                                else -> Unit
                            }
                            resetDragState(state)
                            break
                        }
                        val current = change.position
                        val delta = current - lastPosition
                        lastPosition = current
                        totalDrag += delta
                        if (mode == DragMode.None) {
                            if (abs(totalDrag.x) < DIRECTION_LOCK_THRESHOLD_PX &&
                                abs(totalDrag.y) < DIRECTION_LOCK_THRESHOLD_PX
                            ) continue
                            val candidate = pickDragMode(
                                dragAmount = totalDrag,
                                startX = startX,
                                width = size.width.toFloat(),
                            )
                            val allowed = when (candidate) {
                                DragMode.Seek -> config.swipeSeekEnabled
                                DragMode.Brightness,
                                DragMode.Volume,
                                -> isFullscreen && config.swipeBrightnessVolumeEnabled
                                DragMode.FullscreenEnter -> !isFullscreen
                                DragMode.FullscreenExit -> isFullscreen && fullscreenExitGestureEnabled
                                DragMode.None -> false
                            }
                            if (!allowed) continue
                            mode = candidate
                            onGestureFeedback()
                            state.dragMode.value = candidate
                            when (candidate) {
                                DragMode.Brightness -> state.brightnessOverlayActive.value = true
                                DragMode.Volume -> state.volumeOverlayActive.value = true
                                DragMode.Seek -> state.seekDragOverlayActive.value = true
                                DragMode.FullscreenEnter -> Unit
                                DragMode.FullscreenExit -> Unit
                                DragMode.None -> Unit
                            }
                        }
                        if (mode != DragMode.None) {
                            change.consume()
                            handleDragMode(
                                player = player,
                                state = state,
                                mode = mode,
                                delta = delta,
                                levelDragRangePx = (size.height * 0.72f).coerceAtLeast(320f),
                                onAdjustBrightness = onAdjustBrightness,
                                onAdjustVolume = onAdjustVolume,
                            )
                        }
                    }
                }
            }
            .pointerInput(player, config) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = { offset ->
                        val action = doubleTapAction(offset.x, size.width.toFloat())
                        if (!action.isEnabled(config.doubleTapSeekEnabled)) {
                            return@detectTapGestures
                        }
                        onGestureFeedback()
                        when (action) {
                            PlayerDoubleTapAction.Rewind -> {
                                player.seekTo(
                                    (player.currentPosition - config.seekIncrementMs)
                                        .coerceAtLeast(0L),
                                )
                                state.showSeekHint(GestureSide.Left, config.doubleTapSeekSeconds)
                            }
                            PlayerDoubleTapAction.TogglePlayback -> {
                                if (player.isPlaying) player.pause() else player.play()
                                state.showPlaybackHint(player.isPlaying)
                            }
                            PlayerDoubleTapAction.Forward -> {
                                player.seekTo(player.currentPosition + config.seekIncrementMs)
                                state.showSeekHint(GestureSide.Right, config.doubleTapSeekSeconds)
                            }
                        }
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
            .then(
                if (!isFullscreen) {
                    Modifier
                } else {
                    Modifier.pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (abs(zoom - 1f) > 0.05f) {
                                state.resizeMode.value = if (zoom > 1f) ResizeMode.Crop else ResizeMode.Fit
                            }
                        }
                    }
                },
            ),
    ) {
        SeekHintOverlay(state = state)
        PlaybackHintOverlay(state = state)
        PlayerLevelOverlay(
            visible = state.brightnessOverlayActive.value,
            fraction = state.brightnessFraction.floatValue,
            label = stringResource(R.string.player_gesture_brightness),
            icon = Icons.Filled.Brightness6,
            side = GestureSide.Left,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        PlayerLevelOverlay(
            visible = state.volumeOverlayActive.value,
            fraction = state.volumeFraction.floatValue,
            label = stringResource(R.string.player_gesture_volume),
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            side = GestureSide.Right,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
        SeekDragOverlay(state = state, durationMs = player.duration)
        SpeedBoostBadge(visible = state.longPressBoostActive.value, factor = LONG_PRESS_SPEED_FACTOR)
    }
}

private val PlayerGestureConfig.seekIncrementMs: Long
    get() = doubleTapSeekSeconds.coerceIn(5, 30) * 1_000L

private fun handleDragMode(
    player: Player,
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
        DragMode.Seek -> {
            val deltaMs = (delta.x * SEEK_DRAG_MS_PER_PIXEL).toLong()
            val duration = if (player.duration > 0) player.duration else Long.MAX_VALUE
            state.seekDragTargetMs.longValue =
                (state.seekDragTargetMs.longValue + deltaMs).coerceIn(0L, duration)
        }
        DragMode.FullscreenEnter,
        DragMode.FullscreenExit,
        DragMode.None,
        -> Unit
    }
}

internal fun adjustLevelFraction(current: Float, deltaY: Float, dragRangePx: Float): Float =
    (current - deltaY / dragRangePx.coerceAtLeast(1f)).coerceIn(0f, 1f)

private fun pickDragMode(dragAmount: Offset, startX: Float, width: Float): DragMode = when {
    abs(dragAmount.x) > abs(dragAmount.y) -> DragMode.Seek
    startX in (width * 0.35f)..(width * 0.65f) && dragAmount.y < 0f -> DragMode.FullscreenEnter
    startX in (width * 0.35f)..(width * 0.65f) && dragAmount.y > 0f -> DragMode.FullscreenExit
    startX < width / 2f -> DragMode.Brightness
    else -> DragMode.Volume
}

private fun resetDragState(state: PlayerGestureState) {
    state.dragMode.value = DragMode.None
    state.brightnessOverlayActive.value = false
    state.volumeOverlayActive.value = false
    state.seekDragOverlayActive.value = false
}
