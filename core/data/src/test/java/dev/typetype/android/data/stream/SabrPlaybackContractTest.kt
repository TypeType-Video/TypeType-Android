package dev.typetype.android.data.stream

import dev.typetype.android.data.network.dto.SabrPlaybackResponse
import dev.typetype.android.data.network.dto.SabrLivePlaybackDto
import dev.typetype.android.data.network.dto.SabrPlaybackWindowResponseDto
import dev.typetype.android.data.network.dto.SabrPlaybackWindowSegmentDto
import dev.typetype.android.data.network.dto.SabrPlaybackWindowTrackDto
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import dev.typetype.android.domain.stream.StreamRequestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SabrPlaybackContractTest {
    @Test
    fun terminalWindowAcceptsEmptyValidatedTracks() {
        val session = SabrPlaybackWindowResponseDto(
            sessionId = SESSION_ID,
            generation = 0,
            ready = true,
            durationMs = 120_000,
            endOfStream = true,
            audio = emptyTrack(140, "audio"),
            video = emptyTrack(137, "video"),
            startTimeMs = 120_500,
        ).requireWindowResponse(BASE_URL, target(), control())

        assertTrue(session.endOfStream)
        assertTrue(requireNotNull(session.audioWindow).segments.isEmpty())
        assertTrue(requireNotNull(session.videoWindow).segments.isEmpty())
        assertEquals(120_000, session.startTimeMs)
        assertEquals(120_000, session.windowEndMs)
    }

    @Test
    fun activeWindowRejectsEmptyTracks() {
        val failure = runCatching {
            SabrPlaybackWindowResponseDto(
                sessionId = SESSION_ID,
                generation = 0,
                ready = true,
                durationMs = 120_000,
                endOfStream = false,
                audio = emptyTrack(140, "audio"),
                video = emptyTrack(137, "video"),
            ).requireWindowResponse(BASE_URL, target(), control())
        }.exceptionOrNull()

        assertTrue(failure?.message.orEmpty().contains("empty audio window"))
    }

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

    @Test
    fun activeLiveWindowAcceptsTheFutureSegmentPastTheReportedHead() {
        val live = liveWindow()
        val session = SabrPlaybackWindowResponseDto(
            sessionId = SESSION_ID,
            generation = 0,
            ready = true,
            durationMs = LIVE_HEAD_MS,
            endOfStream = false,
            audio = liveTrack(140, "audio"),
            video = liveTrack(137, "video"),
            startTimeMs = LIVE_HEAD_MS - 10_000L,
            live = live,
        ).requireWindowResponse(
            baseUrl = BASE_URL,
            target = target(isLive = true),
            control = control(
                startTimeMs = LIVE_HEAD_MS - 10_000L,
                live = live,
            ),
        )

        assertEquals(LIVE_HEAD_MS + 5_000L, session.windowEndMs)
        assertEquals(LIVE_HEAD_MS + 5_000L, session.durationMs)
        assertTrue(requireNotNull(session.live).active)
    }

    @Test
    fun postLiveWindowBecomesABoundedReplay() {
        val live = liveWindow(active = false)
        val durationMs = LIVE_HEAD_MS + 5_000L
        val session = SabrPlaybackWindowResponseDto(
            sessionId = SESSION_ID,
            generation = 0,
            ready = true,
            durationMs = durationMs,
            endOfStream = true,
            audio = liveTrack(140, "audio"),
            video = liveTrack(137, "video"),
            startTimeMs = LIVE_HEAD_MS - 10_000L,
            live = live,
        ).requireWindowResponse(
            baseUrl = BASE_URL,
            target = target(isLive = true),
            control = control(
                startTimeMs = LIVE_HEAD_MS - 10_000L,
                live = live,
            ),
        )

        val postLive = requireNotNull(session.live)
        assertFalse(postLive.active)
        assertTrue(postLive.postLiveDvr)
        assertTrue(session.endOfStream)
        assertEquals(durationMs, session.durationMs)
    }

    @Test
    fun activeLiveTargetRejectsMissingLiveMetadata() {
        val failure = runCatching {
            control().requireControlResponse(target(isLive = true))
        }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertTrue(failure?.message.orEmpty().contains("format tuple"))
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

    private fun emptyTrack(itag: Int, kind: String) = SabrPlaybackWindowTrackDto(
        mime = "$kind/mp4",
        initUrl = "/api/sabr/playback/$SESSION_ID/$itag/init?generation=0",
        segments = emptyList(),
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

    private fun liveTrack(itag: Int, kind: String) = SabrPlaybackWindowTrackDto(
        mime = "$kind/mp4",
        initUrl = "/api/sabr/playback/$SESSION_ID/$itag/init?generation=0",
        segments = (0..2).map { offset ->
            SabrPlaybackWindowSegmentDto(
                url = "/api/sabr/playback/$SESSION_ID/$itag/segment/" +
                    "${100 + offset}?generation=0",
                startMs = LIVE_HEAD_MS - 10_000L + offset * 5_000L,
                durationMs = 5_000L,
            )
        },
    )

    private fun liveWindow(active: Boolean = true) = SabrLivePlaybackDto(
        active = active,
        postLiveDvr = !active,
        headSequence = 103,
        headTimeMs = LIVE_HEAD_MS,
        seekableStartMs = LIVE_HEAD_MS - 43_200_000L,
        seekableEndMs = LIVE_HEAD_MS,
        atLiveEdge = active,
        targetLatencyMs = 20_000L,
    )

    private fun control(
        startTimeMs: Long = 0L,
        live: SabrLivePlaybackDto? = null,
    ) = SabrPlaybackResponse(
        sessionId = SESSION_ID,
        videoId = VIDEO_ID,
        manifestUrl = "/sabr/playback/$SESSION_ID/manifest",
        videoItag = 137,
        audioItag = 140,
        startTimeMs = startTimeMs,
        generation = 0,
        ready = true,
        status = "ready",
        live = live,
    )

    private fun target(isLive: Boolean = false) = SabrPlaybackTarget(
        videoId = VIDEO_ID,
        requestScope = StreamRequestScope("server", "account", BASE_URL),
        videoItag = 137,
        audioItag = 140,
        audioTrackId = null,
        isLive = isLive,
    )

    private companion object {
        const val BASE_URL = "https://beta.typetype.video/api/"
        const val SESSION_ID = "session"
        const val VIDEO_ID = "video"
        const val LIVE_HEAD_MS = 5_338_444_800L
    }
}
