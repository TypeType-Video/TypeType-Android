package dev.typetype.android.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Stable
internal class PlayerFullscreenExitGestureState {
    var progress by mutableFloatStateOf(0f)
        private set

    fun update(value: Float) {
        progress = value.coerceIn(0f, 1f)
    }

    fun reset() {
        progress = 0f
    }
}

@Composable
internal fun rememberPlayerFullscreenExitGestureState(): PlayerFullscreenExitGestureState =
    remember { PlayerFullscreenExitGestureState() }

@Composable
internal fun PlayerFullscreenExitGestureBox(
    enabled: Boolean,
    onGestureFeedback: () -> Unit,
    onExitFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPlayerFullscreenExitGestureState()
    val currentExitFullscreen by rememberUpdatedState(onExitFullscreen)
    LaunchedEffect(enabled) {
        if (!enabled) state.reset()
    }
    Box(
        modifier = modifier
            .background(Color.Black)
            .clipToBounds()
            .playerFullscreenExitGesture(
                enabled = enabled,
                state = state,
                onGestureFeedback = onGestureFeedback,
                onExitFullscreen = { currentExitFullscreen() },
            )
            .graphicsLayer {
                val scale = 1f - state.progress * 0.12f
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0.5f, 1f)
            },
        content = content,
    )
}

internal fun Modifier.playerFullscreenExitGesture(
    enabled: Boolean,
    state: PlayerFullscreenExitGestureState,
    onGestureFeedback: () -> Unit,
    onExitFullscreen: () -> Unit,
): Modifier = if (!enabled) {
    this
} else {
    pointerInput(state, enabled) {
        val directionThresholdPx = 18.dp.toPx()
        val completionThresholdPx = 64.dp.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Initial,
            )
            val startsInMiddle = down.position.x in (size.width * 0.35f)..(size.width * 0.65f)
            var lastPosition = down.position
            var totalDrag = Offset.Zero
            var locked = false
            var completed = false
            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    val delta = change.position - lastPosition
                    lastPosition = change.position
                    totalDrag += delta
                    if (!locked) {
                        if (abs(totalDrag.x) < directionThresholdPx &&
                            abs(totalDrag.y) < directionThresholdPx
                        ) continue
                        if (!startsInMiddle || totalDrag.y <= abs(totalDrag.x)) break
                        locked = true
                        onGestureFeedback()
                    }
                    if (!change.pressed) {
                        completed = totalDrag.y >= completionThresholdPx
                        break
                    }
                    change.consume()
                    state.update(totalDrag.y / completionThresholdPx)
                }
            } finally {
                if (completed) {
                    state.reset()
                    onExitFullscreen()
                } else {
                    state.reset()
                }
            }
        }
    }
}
