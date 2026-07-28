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
    fun playbackStartsWithoutWaitingForTheWholeNetworkReserve() {
        val fixture = fixture()
        fixture.loadControl.onPrepared(fixture.playerId)

        try {
            assertFalse(
                fixture.loadControl.shouldStartPlayback(
                    fixture.parameters(bufferedDurationUs = 1_999_999L),
                ),
            )
            assertTrue(
                fixture.loadControl.shouldStartPlayback(
                    fixture.parameters(bufferedDurationUs = 2_000_000L),
                ),
            )
            assertFalse(
                fixture.loadControl.shouldStartPlayback(
                    fixture.parameters(bufferedDurationUs = 2_999_999L, rebuffering = true),
                ),
            )
            assertTrue(
                fixture.loadControl.shouldStartPlayback(
                    fixture.parameters(bufferedDurationUs = 3_000_000L, rebuffering = true),
                ),
            )
        } finally {
            fixture.loadControl.onReleased(fixture.playerId)
        }
    }

    @Test
    fun timeThresholdOwnsStreamingBufferDecisions() {
        val fixture = fixture()
        fixture.loadControl.onPrepared(fixture.playerId)
        fixture.loadControl.onTracksSelected(
            fixture.parameters(bufferedDurationUs = 0L),
            TrackGroupArray.EMPTY,
            emptyArray<ExoTrackSelection>(),
        )
        val allocator = fixture.loadControl.getAllocator(fixture.playerId)
        val allocations = buildList {
            while (allocator.totalBytesAllocated < 32 * 1024 * 1024) {
                add(allocator.allocate())
            }
        }

        try {
            assertTrue(
                fixture.loadControl.shouldContinueLoading(
                    fixture.parameters(bufferedDurationUs = 10_000_000L),
                ),
            )
            assertFalse(
                fixture.loadControl.shouldContinueLoading(
                    fixture.parameters(bufferedDurationUs = 30_000_000L),
                ),
            )
        } finally {
            allocations.forEach(allocator::release)
            fixture.loadControl.onReleased(fixture.playerId)
        }
    }

    private fun fixture(): Fixture {
        val playerId = PlayerId("load-control-test")
        val timeline = SinglePeriodTimeline(
            60_000_000L,
            true,
            false,
            false,
            null,
            MediaItem.fromUri("https://instance.test/media"),
        )
        return Fixture(
            loadControl = createPlaybackLoadControl(),
            playerId = playerId,
            timeline = timeline,
            periodId = MediaSource.MediaPeriodId(timeline.getUidOfPeriod(0)),
        )
    }

    private data class Fixture(
        val loadControl: androidx.media3.exoplayer.DefaultLoadControl,
        val playerId: PlayerId,
        val timeline: Timeline,
        val periodId: MediaSource.MediaPeriodId,
    ) {
        fun parameters(
            bufferedDurationUs: Long,
            rebuffering: Boolean = false,
        ) = LoadControl.Parameters(
            playerId,
            timeline,
            periodId,
            0L,
            bufferedDurationUs,
            1f,
            true,
            rebuffering,
            C.TIME_UNSET,
            C.TIME_UNSET,
        )
    }
}
