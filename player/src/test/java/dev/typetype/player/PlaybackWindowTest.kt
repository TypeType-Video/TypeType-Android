package dev.typetype.player

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Timeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackWindowTest {
    @Test
    fun windowKeepsIndependentAudioAndVideoTimelines() {
        val window = PlaybackWindow(
            generation = 3,
            durationUs = 60_000_000,
            startPositionUs = 10_000_000,
            endOfStream = false,
            audio = track(PlaybackTrackKind.Audio, "140", 9_984_000),
            video = track(PlaybackTrackKind.Video, "137", 10_000_000),
        )

        assertEquals(19_984_000, window.audio.endPositionUs)
        assertEquals(20_000_000, requireNotNull(window.video).endPositionUs)
    }

    @Test
    fun trackRejectsAnEmptySegmentWindow() {
        assertThrows(IllegalArgumentException::class.java) {
            PlaybackTrack(
                kind = PlaybackTrackKind.Audio,
                id = "140",
                mimeType = "audio/mp4",
                initializationUrl = "https://example.test/140/init",
                segments = emptyList(),
            )
        }
    }

    @Test
    fun bufferedRangeRejectsAnInvertedTimeline() {
        assertThrows(IllegalArgumentException::class.java) {
            PlaybackBufferedRange("140", 2_000, 1_000)
        }
    }

    @Test
    fun playbackWindowRefreshWaitsUntilBufferAndAvailableMediaAreLow() {
        assertFalse(
            shouldRefreshPlaybackWindow(
                playbackPositionUs = 10_000_000,
                bufferedEndUs = 15_000_000,
                availableEndUs = 15_000_000,
                endOfStream = false,
                bufferGoalUs = 10_000_000,
            ),
        )
        assertFalse(
            shouldRefreshPlaybackWindow(
                playbackPositionUs = 10_000_000,
                bufferedEndUs = 14_999_999,
                availableEndUs = 20_000_000,
                endOfStream = false,
                bufferGoalUs = 10_000_000,
            ),
        )
        assertTrue(
            shouldRefreshPlaybackWindow(
                playbackPositionUs = 10_000_000,
                bufferedEndUs = 14_999_999,
                availableEndUs = 14_999_999,
                endOfStream = false,
                bufferGoalUs = 10_000_000,
            ),
        )
        assertFalse(shouldRefreshPlaybackWindow(30_000_000, 30_000_000, 30_000_000, true))
    }

    @Test
    fun satisfiedBufferGoalDoesNotTriggerRefreshOnEveryPoll() {
        val windowEndUs = 10_000_000L

        assertFalse(shouldRefreshPlaybackWindow(0L, windowEndUs, windowEndUs, false, 10_000_000L))
        assertFalse(shouldRefreshPlaybackWindow(4_999_999L, windowEndUs, windowEndUs, false, 10_000_000L))
        assertTrue(shouldRefreshPlaybackWindow(5_000_001L, windowEndUs, windowEndUs, false, 10_000_000L))
    }

    @Test
    fun initialBufferGoalUsesItsOwnRefreshWatermark() {
        assertFalse(shouldRefreshPlaybackWindow(0L, 2_500_000L, 2_500_000L, false, 2_500_000L))
        assertTrue(shouldRefreshPlaybackWindow(0L, 1_249_999L, 1_249_999L, false, 2_500_000L))
    }

    @Test
    fun stagnantWindowsBackOffRefreshPollingAndRecoverWhenProgressResumes() {
        val cadence = PlaybackWindowRefreshCadence()

        assertEquals(1_000L, cadence.onWindowAvailable(1_000_000L, 1_000_000L, 5_000_000L))
        assertEquals(2_000L, cadence.onWindowAvailable(1_000_000L, 1_000_000L, 5_000_000L))
        assertEquals(2_000L, cadence.onWindowAvailable(1_000_000L, 1_000_000L, 5_000_000L))
        assertEquals(500L, cadence.onWindowAvailable(1_000_000L, 2_000_000L, 5_000_000L))
        assertEquals(500L, cadence.onWindowAvailable(2_000_000L, 5_000_000L, 5_000_000L))
    }

    @Test
    fun availableWindowAheadIsMeasuredFromTheCurrentPlaybackPosition() {
        val window = PlaybackWindow(
            generation = 0L,
            durationUs = 60_000_000L,
            startPositionUs = 10_000_000L,
            endOfStream = false,
            audio = track(PlaybackTrackKind.Audio, "140", 30_000_000L),
            video = track(PlaybackTrackKind.Video, "137", 30_000_000L),
        )

        assertEquals(20_000_000L, window.availableWindowAheadUs(20_000_000L))
    }

    @Test
    fun seekResetsTheRefreshPollingBackoff() {
        val cadence = PlaybackWindowRefreshCadence()
        cadence.onWindowAvailable(1_000_000L, 1_000_000L, 5_000_000L)
        cadence.onWindowAvailable(1_000_000L, 1_000_000L, 5_000_000L)

        cadence.reset()

        assertEquals(500L, cadence.pollIntervalMs)
    }

    @Test
    fun activeLiveRefreshUsesTheSmallerLiveBufferGoal() {
        assertFalse(
            shouldRefreshPlaybackWindow(
                10_000_000,
                16_000_000,
                16_000_000,
                false,
                activeLive = true,
            ),
        )
        assertTrue(
            shouldRefreshPlaybackWindow(
                10_000_000,
                14_999_999,
                14_999_999,
                false,
                activeLive = true,
            ),
        )
    }

    @Test
    fun bufferedSeekUsesTheActualRetainedMediaBoundary() {
        assertTrue(canSeekWithinPlaybackBuffer(10_000_000, 40_000_000, 67_000_000))
        assertTrue(canSeekWithinPlaybackBuffer(10_000_000, 10_000_000, 67_000_000))
        assertFalse(canSeekWithinPlaybackBuffer(10_000_000, 9_999_999, 67_000_000))
        assertTrue(canSeekWithinPlaybackBuffer(10_000_000, 66_750_000, 67_000_000))
        assertFalse(canSeekWithinPlaybackBuffer(10_000_000, 66_750_001, 67_000_000))
        assertFalse(canSeekWithinPlaybackBuffer(C.TIME_UNSET, 40_000_000, 67_000_000))
    }

    @Test
    fun retainedPlaybackStartsAtTheAudioVideoIntersection() {
        assertEquals(10_000_000L, retainedPlaybackStartUs(listOf(8_000_000L, 10_000_000L)))
        assertEquals(C.TIME_UNSET, retainedPlaybackStartUs(listOf(8_000_000L, C.TIME_UNSET)))
        assertEquals(C.TIME_UNSET, retainedPlaybackStartUs(emptyList()))
    }

    @Test
    fun playbackRangeUsesTheRetainedMediaQueueAfterMseStyleTrimming() {
        assertEquals(20_120_000, playbackBufferStartUs(49_600_000, 20_120_000))
        assertEquals(19_600_000, playbackBufferStartUs(49_600_000, 10_000_000))
        assertEquals(19_600_000, playbackBufferStartUs(49_600_000, C.TIME_UNSET))
    }

    @Test
    fun activeLiveWindowPublishesASlidingTimelineAtTheTargetLatency() {
        val live = PlaybackLiveWindow(
            active = true,
            postLiveDvr = false,
            headPositionUs = 5_338_444_800_000L,
            seekableStartPositionUs = 5_295_244_800_000L,
            seekableEndPositionUs = 5_338_444_800_000L,
            atLiveEdge = true,
            targetLatencyUs = 20_000_000L,
        )
        val audio = liveTrack(PlaybackTrackKind.Audio, "140", live.headPositionUs)
        val video = liveTrack(PlaybackTrackKind.Video, "137", live.headPositionUs)
        val playbackWindow = PlaybackWindow(
            generation = 0L,
            durationUs = live.headPositionUs + 5_000_000L,
            startPositionUs = live.headPositionUs - 10_000_000L,
            endOfStream = false,
            audio = audio,
            video = video,
            live = live,
        )

        val timeline = playbackWindow.toTimeline(
            MediaItem.Builder().setMediaId("live").build(),
        )
        val window = timeline.getWindow(0, Timeline.Window())
        val period = timeline.getPeriod(0, Timeline.Period())

        assertTrue(window.isLive)
        assertTrue(window.isDynamic)
        assertTrue(window.isSeekable)
        assertEquals(43_200_000L, window.durationMs)
        assertEquals(43_180_000L, window.defaultPositionMs)
        assertEquals(live.seekableStartPositionUs / 1_000L, window.positionInFirstPeriodMs)
        assertEquals(20_000L, requireNotNull(window.liveConfiguration).targetOffsetMs)
        assertEquals(live.headPositionUs + 5_000_000L, period.durationUs)
    }

    @Test
    fun postLiveDvrPublishesAStaticSeekableTimeline() {
        val live = PlaybackLiveWindow(
            active = false,
            postLiveDvr = true,
            headPositionUs = 120_000_000L,
            seekableStartPositionUs = 0L,
            seekableEndPositionUs = 120_000_000L,
            atLiveEdge = false,
            targetLatencyUs = 20_000_000L,
        )
        val playbackWindow = PlaybackWindow(
            generation = 1L,
            durationUs = 120_000_000L,
            startPositionUs = 90_000_000L,
            endOfStream = true,
            audio = track(PlaybackTrackKind.Audio, "140", 30_000_000L),
            video = track(PlaybackTrackKind.Video, "137", 30_000_000L),
            live = live,
        )

        val timeline = playbackWindow.toTimeline(
            MediaItem.Builder().setMediaId("post-live").build(),
        )
        val window = timeline.getWindow(0, Timeline.Window())

        assertFalse(window.isLive)
        assertFalse(window.isDynamic)
        assertTrue(window.isSeekable)
        assertNull(window.liveConfiguration)
        assertEquals(120_000L, window.durationMs)
    }

    private fun track(
        kind: PlaybackTrackKind,
        id: String,
        durationUs: Long,
    ) = PlaybackTrack(
        kind = kind,
        id = id,
        mimeType = if (kind == PlaybackTrackKind.Audio) {
            "audio/mp4; codecs=\"mp4a.40.2\""
        } else {
            "video/mp4; codecs=\"avc1.640028\""
        },
        initializationUrl = "https://example.test/$id/init",
        segments = listOf(
            PlaybackSegment(
                url = "https://example.test/$id/segment/1",
                startPositionUs = 10_000_000,
                durationUs = durationUs,
            ),
        ),
    )

    private fun liveTrack(
        kind: PlaybackTrackKind,
        id: String,
        headPositionUs: Long,
    ) = PlaybackTrack(
        kind = kind,
        id = id,
        mimeType = if (kind == PlaybackTrackKind.Audio) "audio/mp4" else "video/mp4",
        initializationUrl = "https://example.test/$id/init",
        segments = listOf(
            PlaybackSegment(
                url = "https://example.test/$id/segment/1",
                startPositionUs = headPositionUs,
                durationUs = 5_000_000L,
            ),
        ),
    )
}
