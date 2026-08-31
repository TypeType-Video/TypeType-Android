package video.typetype.tv.ui

internal data class CommentTimestamp(
    val label: String,
    val seconds: Long,
)

internal fun String.commentTimestamps(): List<CommentTimestamp> = TIMESTAMP_PATTERN.findAll(this)
    .mapNotNull { match ->
        val hours = match.groupValues[1].toLongOrNull() ?: 0L
        val minutes = match.groupValues[2].toLongOrNull() ?: return@mapNotNull null
        val seconds = match.groupValues[3].toLongOrNull() ?: return@mapNotNull null
        if (minutes > 59L || seconds > 59L) return@mapNotNull null
        CommentTimestamp(match.value, hours * 3_600L + minutes * 60L + seconds)
    }
    .distinctBy { it.seconds }
    .take(MAX_COMMENT_TIMESTAMPS)
    .toList()

private val TIMESTAMP_PATTERN = Regex("""(?<![\w:])(?:(\d{1,2}):)?(\d{1,2}):([0-5]\d)(?![\w:])""")
private const val MAX_COMMENT_TIMESTAMPS = 8
