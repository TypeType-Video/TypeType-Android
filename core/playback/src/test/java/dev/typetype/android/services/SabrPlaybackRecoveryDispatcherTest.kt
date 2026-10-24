package dev.typetype.android.services

import dev.typetype.android.data.stream.SabrPlaybackRecoveryException
import org.junit.Assert.assertEquals
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

    @Test
    fun `requests for one session share the active recovery`() {
        val dispatcher = SabrPlaybackRecoveryDispatcher()
        val results = mutableListOf<Result<Unit>>()
        var listenerCalls = 0
        var finish: ((Result<Unit>) -> Unit)? = null
        dispatcher.setListener { _, _, complete ->
            listenerCalls++
            finish = complete
        }

        dispatcher.request("session", recoveryFailure(), results::add)
        dispatcher.request("session", recoveryFailure(), results::add)
        requireNotNull(finish)(Result.success(Unit))

        assertEquals(1, listenerCalls)
        assertEquals(2, results.size)
        assertTrue(results.all(Result<Unit>::isSuccess))
    }

    @Test
    fun `detaching listener fails every pending request`() {
        val dispatcher = SabrPlaybackRecoveryDispatcher()
        val failure = recoveryFailure()
        val results = mutableListOf<Result<Unit>>()
        dispatcher.setListener { _, _, _ -> }
        dispatcher.request("session", failure, results::add)
        dispatcher.request("session", failure, results::add)

        dispatcher.setListener(null)

        assertEquals(2, results.size)
        assertTrue(results.all { it.exceptionOrNull() === failure })
    }

    private fun recoveryFailure() = SabrPlaybackRecoveryException(
        message = "retry",
        action = "retry_fresh_session_lower_video_itag",
        retryVideoItags = listOf(136),
    )
}
