package dev.typetype.android.services

import dev.typetype.android.data.stream.SabrPlaybackRecoveryException
import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackSession
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SabrPlaybackFailureRecoveryTest {
    @Test
    fun `only session recovery HTTP statuses are accepted`() {
        listOf(202, 404, 409, 410).forEach { status ->
            assertTrue(status.isRecoverableSabrSessionStatus())
        }
        listOf(400, 401, 403, 429, 500, 503).forEach { status ->
            assertFalse(status.isRecoverableSabrSessionStatus())
        }
    }

    @Test
    fun `automatic recovery is deduplicated and bounded twice per active media`() {
        val gate = SabrPlaybackRecoveryGate()

        assertEquals(SabrPlaybackRecoveryDecision.Recover, gate.begin("video", "session-1"))
        assertTrue(gate.takeAttempt())
        assertEquals(SabrPlaybackRecoveryDecision.Ignore, gate.begin("video", "session-1"))
        gate.finish("session-1")
        assertEquals(SabrPlaybackRecoveryDecision.Recover, gate.begin("video", "session-2"))
        assertTrue(gate.takeAttempt())
        gate.finish("session-2")
        assertEquals(SabrPlaybackRecoveryDecision.Exhausted, gate.begin("video", "session-3"))
        assertFalse(gate.takeAttempt())
        gate.transition(null)
        assertEquals(SabrPlaybackRecoveryDecision.Exhausted, gate.begin("video", "session-4"))
        gate.transition("another-video")
        assertEquals(
            SabrPlaybackRecoveryDecision.Recover,
            gate.begin("another-video", "session-5"),
        )
    }

    @Test
    fun `fresh SABR session resets recovery for the same media`() {
        val gate = SabrPlaybackRecoveryGate()

        assertEquals(SabrPlaybackRecoveryDecision.Recover, gate.begin("video", "session-1"))
        assertTrue(gate.takeAttempt())
        gate.finish("session-1")
        assertEquals(SabrPlaybackRecoveryDecision.Recover, gate.begin("video", "session-2"))
        assertTrue(gate.takeAttempt())
        gate.finish("session-2")
        gate.transition("video", startsNewSession = true)

        assertEquals(SabrPlaybackRecoveryDecision.Recover, gate.begin("video", "session-3"))
    }

    @Test
    fun `stable playback rearms recovery after thirty seconds`() {
        val gate = exhaustedGate()

        gate.observeProgress("video", 0L, 0L)
        repeat(30) { second ->
            gate.observeProgress(
                mediaId = "video",
                positionMs = (second + 1) * 1_000L,
                nowMs = (second + 1) * 1_000L,
            )
        }

        assertEquals(SabrPlaybackRecoveryDecision.Recover, gate.begin("video", "session-3"))
    }

    @Test
    fun `stalled progress does not rearm recovery early`() {
        val gate = exhaustedGate()

        gate.observeProgress("video", 0L, 0L)
        gate.observeProgress("video", 1_000L, 1_000L)
        gate.observeProgress("video", 1_500L, 5_000L)
        repeat(29) { second ->
            gate.observeProgress(
                mediaId = "video",
                positionMs = 1_500L + (second + 1) * 1_000L,
                nowMs = 5_000L + (second + 1) * 1_000L,
            )
        }

        assertEquals(SabrPlaybackRecoveryDecision.Exhausted, gate.begin("video", "session-3"))
        gate.observeProgress("video", 31_500L, 35_000L)
        assertEquals(SabrPlaybackRecoveryDecision.Recover, gate.begin("video", "session-4"))
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

    private fun exhaustedGate() = SabrPlaybackRecoveryGate().apply {
        assertEquals(SabrPlaybackRecoveryDecision.Recover, begin("video", "session-1"))
        assertTrue(takeAttempt())
        finish("session-1")
        assertEquals(SabrPlaybackRecoveryDecision.Recover, begin("video", "session-2"))
        assertTrue(takeAttempt())
        finish("session-2")
    }
}
