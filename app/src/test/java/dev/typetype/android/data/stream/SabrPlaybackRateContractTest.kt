package dev.typetype.android.data.stream

import dev.typetype.android.data.network.dto.SabrPlaybackResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class SabrPlaybackRateContractTest {
    @Test
    fun fasterPlaybackExpandsTheRequestedBuffer() {
        val request = response().windowRequest(emptyList(), playbackRate = 2.0f)

        assertEquals(2.0f, request.playbackRate)
        assertEquals(60_000L, request.bufferGoalMs)
    }

    @Test
    fun slowerPlaybackKeepsTheBaselineBuffer() {
        val request = response().windowRequest(emptyList(), playbackRate = 0.5f)

        assertEquals(0.5f, request.playbackRate)
        assertEquals(30_000L, request.bufferGoalMs)
    }

    @Test
    fun unsupportedPlaybackRateFallsBackToNormalSpeed() {
        val request = response().windowRequest(emptyList(), playbackRate = Float.NaN)

        assertEquals(1.0f, request.playbackRate)
        assertEquals(30_000L, request.bufferGoalMs)
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
