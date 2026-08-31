package video.typetype.tv.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

internal fun Modifier.handlePlayerKeys(
    controlsVisible: Boolean,
    interactiveOverlayVisible: Boolean,
    hasQueue: Boolean,
    onControlsInteraction: () -> Unit,
    onShowQueue: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onShowControls: () -> Unit,
): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    if (interactiveOverlayVisible) return@onPreviewKeyEvent false
    if (controlsVisible) onControlsInteraction()
    when (event.key) {
        Key.DirectionUp -> {
            if (controlsVisible || !hasQueue) false else {
                onShowQueue()
                true
            }
        }
        Key.DirectionLeft -> {
            if (controlsVisible) false else {
                onSeekBack()
                onShowControls()
                true
            }
        }
        Key.DirectionRight -> {
            if (controlsVisible) false else {
                onSeekForward()
                onShowControls()
                true
            }
        }
        Key.DirectionCenter, Key.Enter -> {
            if (controlsVisible) false else {
                onShowControls()
                true
            }
        }
        else -> false
    }
}
