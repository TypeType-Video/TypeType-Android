package dev.typetype.android.feature.player.components

import dev.typetype.android.domain.comments.BulletComment
import dev.typetype.android.domain.comments.BulletCommentPosition

internal data class PresentedBulletComment(
    val comment: BulletComment,
    val lane: Int,
    val progress: Float,
)

internal fun presentBulletComments(
    comments: List<BulletComment>,
    positionMillis: Long,
    speed: Float,
    laneCount: Int,
): List<PresentedBulletComment> {
    if (laneCount <= 0) return emptyList()
    val boundedSpeed = speed.coerceIn(0.5f, 2f)
    val regularDuration = (REGULAR_DISPLAY_MILLIS / boundedSpeed).toLong()
    val earliestTime = (positionMillis - maxOf(regularDuration, STATIC_DISPLAY_MILLIS))
        .coerceAtLeast(0L)
    val visible = ArrayList<PresentedBulletComment>(MAX_VISIBLE_COMMENTS)
    var index = comments.lowerBound(earliestTime)
    while (index < comments.size && visible.size < MAX_VISIBLE_COMMENTS) {
        val comment = comments[index]
        if (comment.presentationTimeMillis > positionMillis) break
        if (comment.position != BulletCommentPosition.Bottom) {
            val duration = if (comment.position == BulletCommentPosition.Regular) {
                regularDuration
            } else {
                STATIC_DISPLAY_MILLIS
            }
            val elapsed = positionMillis - comment.presentationTimeMillis
            if (elapsed < duration) {
                val lanes = if (comment.position == BulletCommentPosition.Regular) {
                    laneCount
                } else {
                    minOf(laneCount, STATIC_LANES)
                }
                visible += PresentedBulletComment(
                    comment = comment,
                    lane = index % lanes,
                    progress = elapsed.toFloat() / duration,
                )
            }
        }
        index += 1
    }
    return visible
}

private fun List<BulletComment>.lowerBound(timeMillis: Long): Int {
    var low = 0
    var high = size
    while (low < high) {
        val middle = (low + high) ushr 1
        if (this[middle].presentationTimeMillis < timeMillis) {
            low = middle + 1
        } else {
            high = middle
        }
    }
    return low
}

internal const val DANMAKU_LANES = 8
internal const val REGULAR_DISPLAY_MILLIS = 6_000L
private const val STATIC_DISPLAY_MILLIS = 3_000L
private const val STATIC_LANES = 3
private const val MAX_VISIBLE_COMMENTS = 24
