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
    return comments.asSequence()
        .filter { it.position != BulletCommentPosition.Bottom }
        .mapIndexedNotNull { index, comment ->
            val duration = when (comment.position) {
                BulletCommentPosition.Regular -> (REGULAR_DISPLAY_MILLIS / boundedSpeed).toLong()
                else -> STATIC_DISPLAY_MILLIS
            }
            val elapsed = positionMillis - comment.presentationTimeMillis
            if (elapsed !in 0 until duration) return@mapIndexedNotNull null
            val lanes = if (comment.position == BulletCommentPosition.Regular) {
                laneCount
            } else {
                minOf(laneCount, STATIC_LANES)
            }
            PresentedBulletComment(
                comment = comment,
                lane = index % lanes,
                progress = elapsed.toFloat() / duration,
            )
        }
        .take(MAX_VISIBLE_COMMENTS)
        .toList()
}

internal const val DANMAKU_LANES = 8
internal const val REGULAR_DISPLAY_MILLIS = 6_000L
private const val STATIC_DISPLAY_MILLIS = 3_000L
private const val STATIC_LANES = 3
private const val MAX_VISIBLE_COMMENTS = 24
