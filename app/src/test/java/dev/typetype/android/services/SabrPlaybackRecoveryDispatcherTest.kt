package dev.typetype.android.services

import dev.typetype.android.data.stream.SabrPlaybackRecoveryException
import org.junit.Assert.assertTrue
import org.junit.Test

class SabrPlaybackRecoveryDispatcherTest {
    @Test
    fun `missing listener completes with the recovery failure`() {
        val dispatcher = SabrPlaybackRecoveryDispatcher()
        val failure = recoveryFailure()
        var result: Result<Unit>? = null

        dispatcher.request("session", failure) {
            result = it
        }

        assertTrue(requireNotNull(result).exceptionOrNull() === failure)
    }

    @Test
    fun `active listener owns completion`() {
        val dispatcher = SabrPlaybackRecoveryDispatcher()
        var result: Result<Unit>? = null
        dispatcher.setListener { sessionId, _, complete ->
            assertTrue(sessionId == "session")
            complete(Result.success(Unit))
        }

        dispatcher.request("session", recoveryFailure()) {
            result = it
        }

        assertTrue(requireNotNull(result).isSuccess)
    }

    private fun recoveryFailure() = SabrPlaybackRecoveryException(
        message = "retry",
        action = "retry_fresh_session_lower_video_itag",
        retryVideoItags = listOf(136),
    )
}
