package dev.typetype.android.services

import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackWindowSegment
import dev.typetype.android.domain.stream.SabrPlaybackWindowTrack
import dev.typetype.android.domain.stream.binding
import org.junit.Assert.assertEquals
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
}
