package dev.typetype.android.feature.player.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.media3.common.Player
import dev.typetype.android.domain.stream.SponsorAction
import dev.typetype.android.domain.stream.SponsorBlockSegment
import kotlinx.coroutines.delay

private const val POLL_INTERVAL_MS = 250L
private const val SKIP_GUARD_MS = 200L
private const val END_OF_MEDIA_TOLERANCE_MS = 1_000L

@Composable
internal fun SponsorBlockSkipper(
    player: Player,
    segments: List<SponsorBlockSegment>,
    muteInsteadOfSkip: Boolean = false,
    onSegmentHandled: (SponsorBlockSegment, SponsorBlockAutomaticAction) -> Unit = { _, _ -> },
) {
    val skippable = remember(segments) {
        segments.filter { it.action == SponsorAction.Skip && it.endMs > it.startMs }
    }
    val tracker = remember(player, skippable) { SponsorBlockSkipTracker() }
    if (skippable.isEmpty()) return

    LaunchedEffect(player, skippable, muteInsteadOfSkip) {
        var mutedSegment: SponsorBlockSegment? = null
        var volumeBeforeMute: Float? = null
        try {
            while (true) {
                val positionMs = player.currentPosition
                if (muteInsteadOfSkip) {
                    val activeSegment = skippable.firstOrNull {
                        positionMs >= it.startMs && positionMs < it.endMs
                    }
                    if (activeSegment != mutedSegment) {
                        restoreSponsorBlockVolume(player, volumeBeforeMute)
                        mutedSegment = activeSegment
                        volumeBeforeMute = activeSegment?.let {
                            player.volume.also { volume -> player.volume = 0f }
                        }
                        activeSegment?.let {
                            onSegmentHandled(it, SponsorBlockAutomaticAction.Muted)
                        }
                    }
                } else {
                    val match = tracker.next(
                        positionMs = positionMs,
                        ready = player.playbackState == Player.STATE_READY,
                        segments = skippable,
                    )
                    if (match != null) {
                        player.seekTo(sponsorBlockSkipTargetMs(match, player.duration))
                        onSegmentHandled(match, SponsorBlockAutomaticAction.Skipped)
                    }
                }
                delay(POLL_INTERVAL_MS)
            }
        } finally {
            restoreSponsorBlockVolume(player, volumeBeforeMute)
        }
    }
}

internal enum class SponsorBlockAutomaticAction {
    Skipped,
    Muted,
}

private fun restoreSponsorBlockVolume(player: Player, volumeBeforeMute: Float?) {
    if (volumeBeforeMute != null && player.volume == 0f) {
        player.volume = volumeBeforeMute
    }
}

internal class SponsorBlockSkipTracker {
    private var previousPositionMs: Long? = null
    private var pending: SponsorBlockSegment? = null

    fun next(
        positionMs: Long,
        ready: Boolean,
        segments: List<SponsorBlockSegment>,
    ): SponsorBlockSegment? {
        val previous = previousPositionMs
        previousPositionMs = positionMs
        pending?.let { segment ->
            if (positionMs >= segment.endMs - PENDING_CLEAR_TOLERANCE_MS) {
                pending = null
                return null
            }
            if (positionMs < segment.startMs && ready) {
                pending = null
            } else {
                return null
            }
        }
        val match = segments.firstOrNull { segment ->
            crossedSegmentStart(previous, positionMs, segment) &&
                positionMs <= segment.endMs - SKIP_GUARD_MS
        }
        pending = match
        return match
    }
}

internal fun sponsorBlockSkipTargetMs(
    segment: SponsorBlockSegment,
    durationMs: Long,
): Long {
    if (durationMs <= 0L || durationMs - segment.endMs > END_OF_MEDIA_TOLERANCE_MS) {
        return segment.endMs
    }
    val playableEndMs = minOf(segment.endMs, durationMs - 1L)
    return (playableEndMs - SKIP_GUARD_MS + 1L).coerceAtLeast(segment.startMs)
}

private fun crossedSegmentStart(
    previousPositionMs: Long?,
    positionMs: Long,
    segment: SponsorBlockSegment,
): Boolean = previousPositionMs != null &&
    positionMs > previousPositionMs &&
    previousPositionMs <= segment.startMs + START_CROSSING_TOLERANCE_MS &&
    positionMs >= segment.startMs

private const val START_CROSSING_TOLERANCE_MS = 500L
private const val PENDING_CLEAR_TOLERANCE_MS = 100L
