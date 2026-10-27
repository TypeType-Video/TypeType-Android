package dev.typetype.android.feature.player.components

import android.text.format.DateUtils
import dev.typetype.android.domain.comments.Comment

internal fun formatCommentPublishedTime(
    comment: Comment,
    nowMillis: Long = System.currentTimeMillis(),
): String {
    val publishedAt = comment.publishedAtMillis?.takeIf { it > 0L }
        ?: return normalizeCommentPublishedTime(comment.publishedTime)
    return DateUtils.getRelativeTimeSpanString(
        publishedAt,
        nowMillis,
        DateUtils.SECOND_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
}

internal fun normalizeCommentPublishedTime(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    if (ISO_TIMESTAMP.matches(trimmed)) return ""
    return trimmed.replace(ZULU_SUFFIX, "").trim()
}

private val ISO_TIMESTAMP = Regex(
    pattern = """\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}(?::\d{2}(?:\.\d+)?)?(?:Z|[+-]\d{2}:?\d{2})?""",
    option = RegexOption.IGNORE_CASE,
)

private val ZULU_SUFFIX = Regex(
    pattern = """\s*\(?(?:zulu|utc)\)?\s*$""",
    option = RegexOption.IGNORE_CASE,
)
