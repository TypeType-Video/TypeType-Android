package dev.typetype.android.services

import dev.typetype.android.domain.stream.SabrLivePlaybackWindow
import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackWindowSegment
import dev.typetype.android.domain.stream.SabrPlaybackWindowTrack
import dev.typetype.player.PlaybackSegment
import dev.typetype.player.PlaybackTrack
import dev.typetype.player.PlaybackTrackKind
import dev.typetype.player.PlaybackWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SabrPlaybackWindowLoaderTest {
    @Test
    fun terminalWindowRetainsTheLastValidatedTracks() {
        val previous = PlaybackWindow(
            generation = 0,
            durationUs = 120_000_000,
            startPositionUs = 90_000_000,
            endOfStream = false,
            audio = track(PlaybackTrackKind.Audio, 140),
            video = track(PlaybackTrackKind.Video, 137),
        )
        val terminal = SabrPlaybackSession(
            sessionId = "session",
            manifestUrl = "https://example.test/manifest",
            generation = 0,
            videoItag = 137,
            audioItag = 140,
            audioTrackId = null,
            startTimeMs = 120_000,
            windowEndMs = 120_000,
            durationMs = 120_000,
            endOfStream = true,
            audioWindow = emptyTrack(140, "audio/mp4"),
            videoWindow = emptyTrack(137, "video/mp4"),
        ).toPlayerWindow(previous)

        assertTrue(terminal.endOfStream)
        assertSame(previous.audio, terminal.audio)
        assertSame(previous.video, terminal.video)
        assertEquals(120_000_000, terminal.durationUs)
    }

    @Test
    fun postLiveRefreshKeepsTheSessionAsSeekableReplay() {
        val postLive = SabrPlaybackSession(
            sessionId = "session",
            manifestUrl = "https://example.test/manifest",
            generation = 1,
            videoItag = 137,
            audioItag = 140,
            audioTrackId = null,
            startTimeMs = 90_000,
            windowEndMs = 120_000,
            durationMs = 120_000,
            endOfStream = true,
            audioWindow = windowTrack(140, "audio/mp4"),
            videoWindow = windowTrack(137, "video/mp4"),
            live = SabrLivePlaybackWindow(
                active = false,
                postLiveDvr = true,
                headSequence = 24,
                headTimeMs = 120_000,
                seekableStartMs = 0,
                seekableEndMs = 120_000,
                atLiveEdge = false,
                targetLatencyMs = 20_000,
            ),
        ).toPlayerWindow()

        assertFalse(requireNotNull(postLive.live).active)
        assertTrue(requireNotNull(postLive.live).postLiveDvr)
        assertTrue(postLive.endOfStream)
        assertEquals(120_000_000, postLive.durationUs)
    }

    private fun track(kind: PlaybackTrackKind, itag: Int) = PlaybackTrack(
        kind = kind,
        id = itag.toString(),
        mimeType = if (kind == PlaybackTrackKind.Audio) "audio/mp4" else "video/mp4",
        initializationUrl = "https://example.test/$itag/init",
        segments = listOf(
            PlaybackSegment(
                url = "https://example.test/$itag/segment/1",
                startPositionUs = 90_000_000,
                durationUs = 30_000_000,
            ),
        ),
    )

    private fun emptyTrack(itag: Int, mimeType: String) = SabrPlaybackWindowTrack(
        itag = itag,
        mimeType = mimeType,
        initializationUrl = "https://example.test/$itag/init",
        segments = emptyList(),
    )

    private fun windowTrack(itag: Int, mimeType: String) = SabrPlaybackWindowTrack(
        itag = itag,
        mimeType = mimeType,
        initializationUrl = "https://example.test/$itag/init",
        segments = listOf(
            SabrPlaybackWindowSegment(
                url = "https://example.test/$itag/segment/1",
                startMs = 90_000,
                durationMs = 30_000,
            ),
        ),
    )
}
