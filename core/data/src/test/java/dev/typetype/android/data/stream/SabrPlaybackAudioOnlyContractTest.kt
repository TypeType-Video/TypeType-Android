package dev.typetype.android.data.stream

import dev.typetype.android.data.network.dto.SabrPlaybackResponse
import dev.typetype.android.data.network.dto.SabrPlaybackWindowResponseDto
import dev.typetype.android.data.network.dto.SabrPlaybackWindowSegmentDto
import dev.typetype.android.data.network.dto.SabrPlaybackWindowTrackDto
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import dev.typetype.android.domain.stream.StreamRequestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SabrPlaybackAudioOnlyContractTest {
    @Test
    fun `control and window requests carry audio-only mode`() {
        val target = target()
        val controlRequest = target.controlRequest(45_000)
        val windowRequest = control().windowRequest(emptyList(), audioOnly = true)

        assertTrue(controlRequest.audioOnly)
        assertTrue(windowRequest.audioOnly)
    }

    @Test
    fun `audio-only window accepts audio without video`() {
        val session = window(video = null).requireWindowResponse(
            baseUrl = BASE_URL,
            target = target(),
            control = control(),
        )

        assertEquals(10_000L, session.windowEndMs)
        assertNull(session.videoWindow)
    }

    @Test
    fun `audio-only window rejects an unexpected video track`() {
        val failure = runCatching {
            window(video = track(137, "video")).requireWindowResponse(
                baseUrl = BASE_URL,
                target = target(),
                control = control(),
            )
        }.exceptionOrNull()

        assertTrue(failure?.message.orEmpty().contains("audio-only"))
    }

    private fun window(video: SabrPlaybackWindowTrackDto?) = SabrPlaybackWindowResponseDto(
        sessionId = SESSION_ID,
        generation = 1,
        ready = true,
        durationMs = 120_000,
        endOfStream = false,
        audio = track(140, "audio"),
        video = video,
    )

    private fun track(itag: Int, kind: String) = SabrPlaybackWindowTrackDto(
        mime = "$kind/mp4",
        initUrl = "/api/sabr/playback/$SESSION_ID/$itag/init?generation=1",
        segments = listOf(
            SabrPlaybackWindowSegmentDto(
                url = "/api/sabr/playback/$SESSION_ID/$itag/segment/1?generation=1",
                startMs = 0,
                durationMs = 10_000,
            ),
        ),
    )

    private fun control() = SabrPlaybackResponse(
        sessionId = SESSION_ID,
        videoId = VIDEO_ID,
        manifestUrl = "/sabr/playback/$SESSION_ID/manifest",
        videoItag = 137,
        audioItag = 140,
        generation = 1,
        ready = true,
        status = "ready",
    )

    private fun target() = SabrPlaybackTarget(
        videoId = VIDEO_ID,
        requestScope = StreamRequestScope("server", "account", BASE_URL),
        videoItag = 137,
        audioItag = 140,
        audioTrackId = null,
        audioOnly = true,
    )

    private companion object {
        const val BASE_URL = "https://instance.example/api/"
        const val SESSION_ID = "session"
        const val VIDEO_ID = "video"
    }
}
