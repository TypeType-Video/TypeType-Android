package dev.typetype.android.data.stream

import dev.typetype.android.data.network.dto.EndScreenItem
import dev.typetype.android.domain.stream.EndScreenElement
import dev.typetype.android.domain.stream.EndScreenElementType

internal fun EndScreenItem.toDomainEndScreenElement(): EndScreenElement? {
    val elementType = when (type) {
        "video" -> EndScreenElementType.Video
        "playlist" -> EndScreenElementType.Playlist
        "channel" -> EndScreenElementType.Channel
        "website" -> EndScreenElementType.Website
        else -> return null
    }
    if (id.isBlank() || url.isBlank() || endMs <= startMs) return null
    val placed = left in 0.0..1.0 && top in 0.0..1.0 && left < 1.0 && top < 1.0
    if (!placed || !(width > 0.0) || !(aspectRatio > 0.0)) return null
    return EndScreenElement(
        id = id,
        type = elementType,
        title = title,
        url = url,
        thumbnailUrl = thumbnailUrl,
        startMs = startMs,
        endMs = endMs,
        left = left.toFloat(),
        top = top.toFloat(),
        width = width.toFloat(),
        aspectRatio = aspectRatio.toFloat(),
    )
}
