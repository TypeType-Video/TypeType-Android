package dev.typetype.android.feature.settings.youtubesession

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import dev.typetype.android.domain.youtubesession.KeyEvent
import dev.typetype.android.domain.youtubesession.YoutubeRemoteBrowserInput
import kotlin.math.min
import kotlin.math.roundToInt

internal data class RemoteTextEdit(
    val deleteCount: Int,
    val insertedText: String,
)

internal fun remoteTextEdit(previous: String, next: String): RemoteTextEdit {
    val shared = previous.zip(next).takeWhile { (before, after) -> before == after }.size
    return RemoteTextEdit(
        deleteCount = previous.length - shared,
        insertedText = next.drop(shared),
    )
}

internal fun RemoteTextEdit.toInputs(): List<YoutubeRemoteBrowserInput> = buildList {
    repeat(deleteCount) {
        add(
            YoutubeRemoteBrowserInput.Key(
                event = KeyEvent.Down,
                key = "Backspace",
                code = "Backspace",
                modifiers = emptyList(),
            ),
        )
        add(
            YoutubeRemoteBrowserInput.Key(
                event = KeyEvent.Up,
                key = "Backspace",
                code = "Backspace",
                modifiers = emptyList(),
            ),
        )
    }
    if (insertedText.isNotEmpty()) add(YoutubeRemoteBrowserInput.Text(insertedText))
}

internal fun remotePoint(
    position: Offset,
    containerSize: IntSize,
    frameSize: IntSize,
): IntSize {
    if (containerSize.width <= 0 || containerSize.height <= 0 ||
        frameSize.width <= 0 || frameSize.height <= 0
    ) {
        return IntSize(position.x.roundToInt().coerceAtLeast(0), position.y.roundToInt().coerceAtLeast(0))
    }
    val scale = min(
        containerSize.width.toFloat() / frameSize.width,
        containerSize.height.toFloat() / frameSize.height,
    )
    val offsetX = (containerSize.width - frameSize.width * scale) / 2f
    val offsetY = (containerSize.height - frameSize.height * scale) / 2f
    return IntSize(
        width = ((position.x - offsetX) / scale).roundToInt().coerceIn(0, frameSize.width - 1),
        height = ((position.y - offsetY) / scale).roundToInt().coerceIn(0, frameSize.height - 1),
    )
}

internal fun remoteViewport(size: IntSize): IntSize = IntSize(
    width = size.width.coerceIn(MIN_VIEWPORT_WIDTH, MAX_VIEWPORT_WIDTH),
    height = size.height.coerceIn(MIN_VIEWPORT_HEIGHT, MAX_VIEWPORT_HEIGHT),
)

private const val MIN_VIEWPORT_WIDTH = 320
private const val MIN_VIEWPORT_HEIGHT = 240
private const val MAX_VIEWPORT_WIDTH = 1920
private const val MAX_VIEWPORT_HEIGHT = 1080
