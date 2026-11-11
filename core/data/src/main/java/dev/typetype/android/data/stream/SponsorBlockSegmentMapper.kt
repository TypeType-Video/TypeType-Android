package dev.typetype.android.data.stream

import dev.typetype.android.data.network.dto.SponsorBlockSegmentItem
import dev.typetype.android.domain.stream.SponsorAction
import dev.typetype.android.domain.stream.SponsorBlockSegment
import dev.typetype.android.domain.stream.SponsorCategory
import kotlin.math.roundToLong

internal fun SponsorBlockSegmentItem.toDomainSponsorBlockSegment(
    durationSeconds: Long,
): SponsorBlockSegment? {
    if (!startTime.isFinite() || !endTime.isFinite()) return null
    val scale = if (durationSeconds > 0L && endTime > durationSeconds + 30L) 1.0 else 1_000.0
    val startMs = (startTime * scale).roundToLong().coerceAtLeast(0L)
    val endMs = (endTime * scale).roundToLong().coerceAtLeast(0L)
    if (endMs <= startMs) return null
    return SponsorBlockSegment(
        startMs = startMs,
        endMs = endMs,
        category = SponsorCategory.fromKey(category),
        action = SponsorAction.fromKey(action),
    )
}
