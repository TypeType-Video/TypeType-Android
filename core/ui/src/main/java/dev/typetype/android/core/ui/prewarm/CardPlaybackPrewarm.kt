package dev.typetype.android.core.ui.prewarm

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

class CardPlaybackPrewarm internal constructor(
    internal val prewarm: PlaybackPrewarm,
    internal val settled: SettledCardPrefetch?,
    internal val request: PrewarmRequest,
    internal val key: Any,
)

@Composable
fun rememberCardPlaybackPrewarm(videoUrl: String, isLive: Boolean): CardPlaybackPrewarm? {
    val prewarm = LocalPlaybackPrewarm.current ?: return null
    val settled = LocalSettledCardPrefetch.current
    val key = remember { Any() }
    DisposableEffect(settled, key) {
        onDispose { settled?.remove(key) }
    }
    return remember(prewarm, settled, videoUrl, isLive) {
        CardPlaybackPrewarm(prewarm, settled, PrewarmRequest(videoUrl, isLive), key)
    }
}

fun Modifier.cardPlaybackPrewarm(card: CardPlaybackPrewarm?): Modifier {
    if (card == null) return this
    val placed = card.settled?.let { settled ->
        onGloballyPositioned { coordinates ->
            settled.place(card.key, card.request, coordinates.visibleFraction(), coordinates.boundsInWindow().top)
        }
    } ?: this
    return placed.pointerInput(card) {
        val confirmAfterMillis = viewConfiguration.longPressTimeoutMillis
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val session = card.prewarm.pressSession(card.request)
            val released = withTimeoutOrNull(confirmAfterMillis) { awaitReleaseOrScroll() }
            if (released == false) session.cancel() else session.confirm()
        }
    }
}

private suspend fun AwaitPointerEventScope.awaitReleaseOrScroll(): Boolean {
    while (true) {
        val changes = awaitPointerEvent(PointerEventPass.Final).changes
        if (changes.all { it.changedToUpIgnoreConsumed() }) return true
        if (changes.any { it.isScrollMove() || it.isOutOfBounds(size, extendedTouchPadding) }) return false
    }
}

private fun PointerInputChange.isScrollMove(): Boolean = pressed && previousPressed && isConsumed

private fun LayoutCoordinates.visibleFraction(): Float {
    val area = size.width.toFloat() * size.height
    if (area <= 0f || !isAttached) return 0f
    val visible = boundsInWindow()
    return (visible.width * visible.height / area).coerceIn(0f, 1f)
}
