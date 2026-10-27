package dev.typetype.android.data.stream

import dev.typetype.android.data.network.ServerResponseException
import dev.typetype.android.domain.stream.SabrPlaybackSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SabrMissingSessionTest {
    private val script = SabrServerScript()

    @After
    fun tearDown() = script.shutdown()

    @Test
    fun anEvictedSessionIsReportedAsMissing() = runBlocking {
        script.enqueue(EVICTED_SESSION_BODY, code = 404)

        val failure = runCatching { refresh() }.exceptionOrNull()

        assertTrue(failure is ServerResponseException)
        assertEquals(SABR_PLAYBACK_NOT_FOUND, (failure as ServerResponseException).failureCode)
        assertEquals(404, failure.statusCode)
    }

    @Test
    fun anEvictedSessionDuringSeekIsReportedAsMissing() = runBlocking {
        script.enqueue(EVICTED_SESSION_BODY, code = 404)

        val failure = runCatching {
            SabrPlaybackSessionPreparer(pause = {})
                .seek(script.api, script.baseUrl, script.target(), script.binding(), PLAYER_TIME_MS)
        }.exceptionOrNull()

        assertEquals(SABR_PLAYBACK_NOT_FOUND, (failure as ServerResponseException).failureCode)
    }

    @Test
    fun aMissingVideoKeepsTheServerCode() = runBlocking {
        script.enqueue("""{"error":"Video not found","code":"video_not_found"}""", code = 404)

        val failure = runCatching {
            SabrPlaybackSessionPreparer(pause = {})
                .prepareOnce(script.api, script.baseUrl, script.target(), PLAYER_TIME_MS)
        }.exceptionOrNull()

        assertEquals("video_not_found", (failure as ServerResponseException).failureCode)
    }

    private suspend fun refresh() = SabrPlaybackSessionPreparer(pause = {})
        .refresh(script.api, script.baseUrl, script.target(), script.binding()) {
            SabrPlaybackSnapshot(playerTimeMs = PLAYER_TIME_MS, bufferedRanges = emptyList())
        }

    private companion object {
        const val PLAYER_TIME_MS = 245_000L
        const val EVICTED_SESSION_BODY = """{"error":"No active SABR playback session","code":"error"}"""
    }
}
