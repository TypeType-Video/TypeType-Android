package video.typetype.tv.ui

import video.typetype.sdk.core.Video
import video.typetype.sdk.core.Comment

internal fun Video.relativeUploadDate(nowEpochSeconds: Long = System.currentTimeMillis() / 1_000L): String {
    val published = publishedAtEpochSeconds?.takeIf { it > 0L }
        ?: uploadedAtEpochSeconds.takeIf { it > 0L }
    return relativeDate(uploadDate, published, nowEpochSeconds)
}

internal fun Comment.relativePublishedTime(nowEpochSeconds: Long = System.currentTimeMillis() / 1_000L): String =
    relativeDate(publishedTime, publishedAtEpochSeconds?.takeIf { it > 0L }, nowEpochSeconds)

private fun relativeDate(rawValue: String, published: Long?, nowEpochSeconds: Long): String {
    published ?: return rawValue
    val age = nowEpochSeconds - published
    if (age < 0L) return rawValue
    return when {
        age < MINUTE -> "Just now"
        age < HOUR -> relativeUnit(age / MINUTE, "minute")
        age < DAY -> relativeUnit(age / HOUR, "hour")
        age < MONTH -> relativeUnit(age / DAY, "day")
        age < YEAR -> relativeUnit(age / MONTH, "month")
        else -> relativeUnit(age / YEAR, "year")
    }
}

private fun relativeUnit(value: Long, unit: String): String =
    "$value $unit${if (value == 1L) "" else "s"} ago"

private const val MINUTE = 60L
private const val HOUR = 60L * MINUTE
private const val DAY = 24L * HOUR
private const val MONTH = 30L * DAY
private const val YEAR = 365L * DAY
