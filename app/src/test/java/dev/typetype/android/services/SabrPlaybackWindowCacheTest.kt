package dev.typetype.android.services

import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackWindowSegment
import dev.typetype.android.domain.stream.SabrPlaybackWindowTrack
import dev.typetype.android.domain.stream.binding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SabrPlaybackWindowCacheTest {
    @Test
    fun `audio-only session is retained without a video window`() {
        val session = SabrPlaybackSession(
            sessionId = "session",
            manifestUrl = "https://instance.example/api/sabr/playback/session/manifest",
            generation = 1,
            videoItag = 137,
            audioItag = 140,
            audioTrackId = null,
            audioWindow = SabrPlaybackWindowTrack(
                itag = 140,
                mimeType = "audio/mp4",
                initializationUrl = "https://instance.example/init",
                segments = listOf(
                    SabrPlaybackWindowSegment(
                        url = "https://instance.example/segment",
                        startMs = 0,
                        durationMs = 10_000,
                    ),
                ),
            ),
            videoWindow = null,
        )
        val cache = SabrPlaybackWindowCache()

        cache.put(session)

        assertEquals(session, cache.take(session.binding))
    }

    @Test
    fun `empty terminal window is not retained as an initial window`() {
        val session = SabrPlaybackSession(
            sessionId = "session",
            manifestUrl = "https://instance.example/api/sabr/playback/session/manifest",
            generation = 1,
            videoItag = 137,
            audioItag = 140,
            audioTrackId = null,
            durationMs = 120_000,
            startTimeMs = 120_000,
            windowEndMs = 120_000,
            endOfStream = true,
            audioWindow = SabrPlaybackWindowTrack(
                itag = 140,
                mimeType = "audio/mp4",
                initializationUrl = "https://instance.example/140/init",
                segments = emptyList(),
            ),
            videoWindow = SabrPlaybackWindowTrack(
                itag = 137,
                mimeType = "video/mp4",
                initializationUrl = "https://instance.example/137/init",
                segments = emptyList(),
            ),
        )
        val cache = SabrPlaybackWindowCache()

        cache.put(session)

        assertNull(cache.take(session.binding))
    }
}
