package dev.typetype.android.services

import dev.typetype.android.data.stream.SabrPlaybackRecoveryException
import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackSession
import java.io.IOException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SabrPlaybackFailureRecoveryTest {
    @Test
    fun `only session recovery HTTP statuses are accepted`() {
        listOf(202, 403, 404, 410).forEach { status ->
            assertTrue(status.isRecoverableSabrSessionStatus())
        }
        listOf(400, 401, 429, 500, 503).forEach { status ->
            assertFalse(status.isRecoverableSabrSessionStatus())
        }
    }

    @Test
    fun `automatic recovery is bounded twice per active media`() {
        val gate = SabrPlaybackRecoveryGate()

        assertTrue(gate.acquire("video"))
        assertTrue(gate.acquire("video"))
        assertFalse(gate.acquire("video"))
        gate.transition(null)
        assertFalse(gate.acquire("video"))
        gate.transition("another-video")
        assertTrue(gate.acquire("another-video"))
    }

    @Test
    fun `fresh SABR session resets recovery for the same media`() {
        val gate = SabrPlaybackRecoveryGate()

        assertTrue(gate.acquire("video"))
        assertTrue(gate.acquire("video"))
        gate.transition("video", startsNewSession = true)

        assertTrue(gate.acquire("video"))
    }

    @Test
    fun `replacement session remains recoverable through media wrappers`() {
        val replacement = SabrPlaybackSessionReplacementRequiredException(
            SabrPlaybackSession(
                sessionId = "replacement",
                manifestUrl = "https://instance.example/api/sabr/playback/replacement/manifest",
                generation = 0L,
                videoItag = 136,
                audioItag = 140,
                audioTrackId = "en.0",
            ),
        )
        val wrapped = IOException("source", replacement)

        assertTrue(wrapped.isRecoverableSabrSessionFailure())
        assertTrue(wrapped.sabrSessionReplacementFailure() === replacement)
    }

    @Test
    fun `server recovery action remains recoverable through media wrappers`() {
        val recovery = SabrPlaybackRecoveryException(
            message = "retry",
            action = "retry_fresh_session_lower_video_itag",
            retryVideoItags = listOf(136),
        )

        assertTrue(IOException("source", recovery).isRecoverableSabrSessionFailure())
        assertFalse(
            IOException(
                "source",
                SabrPlaybackRecoveryException("failed", null, emptyList()),
            ).isRecoverableSabrSessionFailure(),
        )
    }

    @Test
    fun `exhausted deferred recovery is not retried by the player listener`() {
        val recovery = SabrPlaybackRecoveryException(
            message = "retry",
            action = "retry_fresh_session_lower_video_itag",
            retryVideoItags = listOf(136),
        )
        val exhausted = SabrPlaybackRecoveryExhaustedException(recovery)

        assertFalse(IOException("source", exhausted).isRecoverableSabrSessionFailure())
    }

    @Test
    fun `lower quality recovery follows the server candidate order`() {
        val target = dev.typetype.android.domain.stream.SabrPlaybackTarget(
            videoId = "video",
            requestScope = dev.typetype.android.domain.stream.StreamRequestScope(
                "server",
                "account",
                "https://instance.example/api/",
            ),
            videoItag = 137,
            audioItag = 140,
            audioTrackId = null,
            recoveryVideoItags = linkedSetOf(136, 135, 134),
        )
        val failure = SabrPlaybackRecoveryException(
            "lower",
            "retry_fresh_session_lower_video_itag",
            listOf(135, 136),
        )

        val recovered = requireNotNull(target.recoveryTarget(failure))

        assertTrue(recovered.videoItag == 135)
        assertFalse(135 in recovered.recoveryVideoItags)
    }

    @Test
    fun `only an external replacement starts a fresh SABR recovery budget`() {
        val first = binding("first", generation = 0L)
        val sought = binding("first", generation = 1L)
        val replacement = binding("second", generation = 0L)

        assertFalse(startsNewSabrPlaybackSession(first, first, continuesCurrentSession = false))
        assertFalse(startsNewSabrPlaybackSession(first, sought, continuesCurrentSession = true))
        assertTrue(startsNewSabrPlaybackSession(first, replacement, continuesCurrentSession = false))
    }

    private fun binding(sessionId: String, generation: Long) = SabrPlaybackBinding(
        sessionId = sessionId,
        generation = generation,
        videoItag = 137,
        audioItag = 140,
        audioTrackId = "en.0",
    )
}
