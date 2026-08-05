package dev.typetype.android.data.comments

import dev.typetype.android.data.network.dto.BulletCommentItem
import dev.typetype.android.domain.comments.BulletComment
import dev.typetype.android.domain.comments.BulletCommentPosition

internal fun BulletCommentItem.toDomain(): BulletComment? {
    val cleanedText = text.trim().take(MAX_TEXT_LENGTH)
    if (cleanedText.isEmpty() || durationMs < 0L) return null
    return BulletComment(
        text = cleanedText,
        rgbColor = argbColor and 0x00FFFFFF,
        position = when (position.uppercase()) {
            "REGULAR" -> BulletCommentPosition.Regular
            "TOP" -> BulletCommentPosition.Top
            "BOTTOM" -> BulletCommentPosition.Bottom
            "SUPERCHAT" -> BulletCommentPosition.SuperChat
            else -> return null
        },
        relativeFontSize = relativeFontSize.toFloat().coerceIn(0.5f, 2f),
        presentationTimeMillis = durationMs,
        isLive = isLive,
    )
}

private const val MAX_TEXT_LENGTH = 180
