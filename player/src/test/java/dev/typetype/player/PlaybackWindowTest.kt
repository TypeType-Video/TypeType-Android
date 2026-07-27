package dev.typetype.player

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun playbackWindowRefreshMatchesTheMseBufferThreshold() {
        assertFalse(shouldRefreshPlaybackWindow(10_000_000, 30_000_000))
        assertTrue(shouldRefreshPlaybackWindow(10_000_000, 29_999_999))
        assertTrue(shouldRefreshPlaybackWindow(30_000_000, 30_000_000))
    }

    @Test
    fun bufferedSeekMatchesTheMseLocalSeekBoundary() {
        assertTrue(canSeekWithinPlaybackBuffer(40_000_000, 40_000_000, 67_000_000))
        assertTrue(canSeekWithinPlaybackBuffer(40_000_000, 10_000_000, 67_000_000))
        assertFalse(canSeekWithinPlaybackBuffer(40_000_000, 9_999_999, 67_000_000))
        assertTrue(canSeekWithinPlaybackBuffer(40_000_000, 66_750_000, 67_000_000))
        assertFalse(canSeekWithinPlaybackBuffer(40_000_000, 66_750_001, 67_000_000))
    }

    @Test
    fun playbackRangeUsesTheRetainedMediaQueueAfterMseStyleTrimming() {
        assertEquals(20_120_000, playbackBufferStartUs(49_600_000, 20_120_000))
        assertEquals(19_600_000, playbackBufferStartUs(49_600_000, 10_000_000))
        assertEquals(19_600_000, playbackBufferStartUs(49_600_000, C.TIME_UNSET))
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
}
