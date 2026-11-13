package dev.typetype.android.domain.stream

enum class EndScreenElementType {
    Video,
    Playlist,
    Channel,
    Website,
}

data class EndScreenElement(
    val id: String,
    val type: EndScreenElementType,
    val title: String,
    val url: String,
    val thumbnailUrl: String,
    val startMs: Long,
    val endMs: Long,
    val left: Float,
    val top: Float,
    val width: Float,
    val aspectRatio: Float,
)

data class EndScreenBox(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

fun List<EndScreenElement>.visibleAt(positionMs: Long): List<EndScreenElement> =
    filter { positionMs >= it.startMs && positionMs < it.endMs }

fun EndScreenElement.boxIn(frameAspectRatio: Float): EndScreenBox {
    val boxWidth = width.coerceAtMost(1f - left)
    val boxHeight = (boxWidth * frameAspectRatio / aspectRatio).coerceAtMost(1f - top)
    return EndScreenBox(left = left, top = top, width = boxWidth, height = boxHeight)
}
