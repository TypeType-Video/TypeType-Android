package dev.typetype.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.SinglePeriodTimeline

@UnstableApi
internal fun PlaybackWindow.toTimeline(mediaItem: MediaItem): Timeline {
    val activeLive = live?.takeIf { it.active } ?: return SinglePeriodTimeline(
        durationUs,
        true,
        false,
        false,
        this,
        mediaItem,
    )
    val windowDurationUs =
        activeLive.seekableEndPositionUs - activeLive.seekableStartPositionUs
    val defaultStartUs = (
        activeLive.defaultStartPositionUs - activeLive.seekableStartPositionUs
    ).coerceIn(0L, windowDurationUs)
    val liveItem = mediaItem.buildUpon()
        .setLiveConfiguration(
            MediaItem.LiveConfiguration.Builder()
                .setTargetOffsetMs((activeLive.targetLatencyUs / 1_000L).coerceAtLeast(1L))
                .build(),
        )
        .build()
    val periodDurationUs = maxOf(
        durationUs,
        activeLive.headPositionUs,
        audio.endPositionUs,
        video?.endPositionUs ?: 0L,
    )
    return SinglePeriodTimeline(
        periodDurationUs,
        windowDurationUs,
        activeLive.seekableStartPositionUs,
        defaultStartUs,
        true,
        true,
        true,
        this,
        liveItem,
    )
}
