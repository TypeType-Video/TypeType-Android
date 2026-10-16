package dev.typetype.android.data.stream

import dev.typetype.android.data.network.dto.SabrPlaybackResponse
import dev.typetype.android.domain.stream.SabrPlaybackBufferedRange
import org.junit.Assert.assertEquals
import org.junit.Test

class SabrPlaybackRateContractTest {
    @Test
    fun fasterPlaybackExpandsTheRequestedBuffer() {
        val request = response().windowRequest(bufferedTracks(), playbackRate = 2.0f)

        assertEquals(2.0f, request.playbackRate)
        assertEquals(20_000L, request.bufferGoalMs)
    }

    @Test
    fun slowerPlaybackKeepsTheBaselineBuffer() {
        val request = response().windowRequest(bufferedTracks(), playbackRate = 0.5f)

        assertEquals(0.5f, request.playbackRate)
        assertEquals(10_000L, request.bufferGoalMs)
    }

    @Test
    fun unsupportedPlaybackRateFallsBackToNormalSpeed() {
        val request = response().windowRequest(emptyList(), playbackRate = Float.NaN)

        assertEquals(1.0f, request.playbackRate)
        assertEquals(2_500L, request.bufferGoalMs)
    }

    @Test
    fun livePlaybackUsesThePlayerLiveBufferGoal() {
        val request = response().windowRequest(
            ranges = emptyList(),
            isLive = true,
            playbackRate = 2.0f,
        )

        assertEquals(2.0f, request.playbackRate)
        assertEquals(16_000L, request.bufferGoalMs)
    }

    @Test
    fun emptyBufferStartsWithAShortWindow() {
        assertEquals(2_500L, response().windowRequest(emptyList()).bufferGoalMs)
    }

    @Test
    fun seekOutsideBufferedTracksStartsWithAShortWindow() {
        val request = response().windowRequest(bufferedTracks(), playerTimeMs = 999_108L)

        assertEquals(2_500L, request.bufferGoalMs)
    }

    @Test
    fun bothSelectedTracksMustCoverThePositionBeforeExpanding() {
        assertEquals(2_500L, response().windowRequest(bufferedTracks().take(1)).bufferGoalMs)
        assertEquals(10_000L, response().windowRequest(bufferedTracks()).bufferGoalMs)
    }

    @Test
    fun unrelatedTracksDoNotExpandTheWindow() {
        val ranges = listOf(SabrPlaybackBufferedRange(303, 0L, 10_000L))

        assertEquals(2_500L, response().windowRequest(ranges).bufferGoalMs)
    }

    @Test
    fun audioOnlyDoesNotRequireVideoCoverage() {
        val request = response().windowRequest(bufferedTracks().takeLast(1), audioOnly = true)

        assertEquals(10_000L, request.bufferGoalMs)
    }

    @Test
    fun initialWindowScalesWithPlaybackSpeed() {
        assertEquals(5_000L, response().windowRequest(emptyList(), playbackRate = 2.0f).bufferGoalMs)
    }

    private fun bufferedTracks() = listOf(
        SabrPlaybackBufferedRange(137, 0L, 10_000L),
        SabrPlaybackBufferedRange(140, 0L, 10_000L),
    )

    private fun response() = SabrPlaybackResponse(
        sessionId = "session",
        videoId = "video",
        videoItag = 137,
        audioItag = 140,
        generation = 0,
        ready = true,
        status = "ready",
    )
}
