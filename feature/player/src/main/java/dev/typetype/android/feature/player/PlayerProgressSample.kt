package dev.typetype.android.feature.player

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

internal data class PlayerProgressSample(val videoUrl: String, val positionMillis: Long)

internal fun playerProgressSample(
    mediaItem: MediaItem?,
    positionMillis: Long,
    durationMillis: Long,
): PlayerProgressSample? {
    val videoUrl = mediaItem?.mediaId?.takeIf { it.isNotBlank() } ?: return null
    if (positionMillis < MIN_PROGRESS_MILLIS) return null
    if (durationMillis > 0 && positionMillis >= (durationMillis * MAX_PROGRESS_FRACTION).toLong()) return null
    return PlayerProgressSample(videoUrl, positionMillis)
}

// A replaced or skipped video reports its last position only in the discontinuity, after the player already
// points at the next one. A video that played to its end is left to the history, not saved as a resume point.
@OptIn(UnstableApi::class)
internal fun discontinuityProgressSample(
    oldPosition: Player.PositionInfo,
    newPosition: Player.PositionInfo,
    reason: Int,
    durationMillis: Long,
): PlayerProgressSample? = when (reason) {
    Player.DISCONTINUITY_REASON_SEEK -> playerProgressSample(newPosition.mediaItem, newPosition.positionMs, durationMillis)
    Player.DISCONTINUITY_REASON_REMOVE,
    Player.DISCONTINUITY_REASON_SKIP,
    -> playerProgressSample(oldPosition.mediaItem, oldPosition.positionMs, 0L)
    else -> null
}

private const val MIN_PROGRESS_MILLIS = 5_000L
private const val MAX_PROGRESS_FRACTION = 0.95
