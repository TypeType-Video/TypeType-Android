package dev.typetype.android.services

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.SinglePeriodTimeline
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@androidx.media3.common.util.UnstableApi
@RunWith(AndroidJUnit4::class)
class PlaybackLoadControlAndroidTest {
    @Test
    fun timeThresholdOwnsStreamingBufferDecisions() {
        val loadControl = createPlaybackLoadControl()
        val playerId = PlayerId("load-control-test")
        val timeline = SinglePeriodTimeline(
            60_000_000L,
            true,
            false,
            false,
            null,
            MediaItem.fromUri("https://instance.test/media"),
        )
        val periodId = MediaSource.MediaPeriodId(timeline.getUidOfPeriod(0))
        loadControl.onPrepared(playerId)
        loadControl.onTracksSelected(
            parameters(playerId, timeline, periodId, bufferedDurationUs = 0L),
            TrackGroupArray.EMPTY,
            emptyArray<ExoTrackSelection>(),
        )
        val allocator = loadControl.getAllocator(playerId)
        val allocations = buildList {
            while (allocator.totalBytesAllocated < 32 * 1024 * 1024) {
                add(allocator.allocate())
            }
        }

        try {
            assertTrue(
                loadControl.shouldContinueLoading(
                    parameters(playerId, timeline, periodId, bufferedDurationUs = 10_000_000L),
                ),
            )
            assertFalse(
                loadControl.shouldContinueLoading(
                    parameters(playerId, timeline, periodId, bufferedDurationUs = 30_000_000L),
                ),
            )
        } finally {
            allocations.forEach(allocator::release)
            loadControl.onReleased(playerId)
        }
    }

    private fun parameters(
        playerId: PlayerId,
        timeline: Timeline,
        periodId: MediaSource.MediaPeriodId,
        bufferedDurationUs: Long,
    ) = LoadControl.Parameters(
        playerId,
        timeline,
        periodId,
        0L,
        bufferedDurationUs,
        1f,
        true,
        false,
        C.TIME_UNSET,
        C.TIME_UNSET,
    )
}
