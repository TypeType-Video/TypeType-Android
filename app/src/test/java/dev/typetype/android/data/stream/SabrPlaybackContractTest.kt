package dev.typetype.android.data.stream

import dev.typetype.android.data.network.dto.SabrPlaybackResponse
import dev.typetype.android.data.network.dto.SabrPlaybackWindowResponseDto
import dev.typetype.android.data.network.dto.SabrPlaybackWindowSegmentDto
import dev.typetype.android.data.network.dto.SabrPlaybackWindowTrackDto
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import dev.typetype.android.domain.stream.StreamRequestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SabrPlaybackContractTest {
    @Test
    fun oneMillisecondTimelineOverlapIsAcceptedAsRounding() {
        val session = window(secondSegmentStartMs = 9_984).requireWindowResponse(
            baseUrl = BASE_URL,
            target = target(),
            control = control(),
        )

        assertEquals(19_969, session.windowEndMs)
    }

    @Test
    fun largerTimelineOverlapIsRejected() {
        val failure = runCatching {
            window(secondSegmentStartMs = 9_983).requireWindowResponse(
                baseUrl = BASE_URL,
                target = target(),
                control = control(),
            )
        }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertTrue(failure?.message.orEmpty().contains("audio timeline"))
    }

    private fun window(secondSegmentStartMs: Long) = SabrPlaybackWindowResponseDto(
        sessionId = SESSION_ID,
        generation = 0,
        ready = true,
        durationMs = 120_000,
        endOfStream = false,
        audio = track(
            itag = 140,
            kind = "audio",
            secondSegmentStartMs = secondSegmentStartMs,
        ),
        video = track(
            itag = 137,
            kind = "video",
            secondSegmentStartMs = 9_985,
        ),
    )

    private fun track(
        itag: Int,
        kind: String,
        secondSegmentStartMs: Long,
    ) = SabrPlaybackWindowTrackDto(
        mime = "$kind/mp4",
        initUrl = "/api/sabr/playback/$SESSION_ID/$itag/init?generation=0",
        segments = listOf(
            segment(itag, sequence = 1, startMs = 0),
            segment(itag, sequence = 2, startMs = secondSegmentStartMs),
        ),
    )

    private fun segment(
        itag: Int,
        sequence: Int,
        startMs: Long,
    ) = SabrPlaybackWindowSegmentDto(
        url = "/api/sabr/playback/$SESSION_ID/$itag/segment/$sequence?generation=0",
        startMs = startMs,
        durationMs = 9_985,
    )

    private fun control() = SabrPlaybackResponse(
        sessionId = SESSION_ID,
        videoId = VIDEO_ID,
        manifestUrl = "/sabr/playback/$SESSION_ID/manifest",
        videoItag = 137,
        audioItag = 140,
        generation = 0,
        ready = true,
        status = "ready",
    )

    private fun target() = SabrPlaybackTarget(
        videoId = VIDEO_ID,
        requestScope = StreamRequestScope("server", "account", BASE_URL),
        videoItag = 137,
        audioItag = 140,
        audioTrackId = null,
    )

    private companion object {
        const val BASE_URL = "https://beta.typetype.video/api/"
        const val SESSION_ID = "session"
        const val VIDEO_ID = "video"
    }
}
