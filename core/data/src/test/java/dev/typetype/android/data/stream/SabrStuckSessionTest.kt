package dev.typetype.android.data.stream

import dev.typetype.android.domain.stream.SabrPlaybackSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SabrStuckSessionTest {
    private val script = SabrServerScript()

    @After
    fun tearDown() = script.shutdown()

    @Test
    fun aSessionStuckWhileRefreshingIsReplacedAtThePlayerPosition() = runBlocking {
        script.enqueuePosition(PLAYER_TIME_MS)
        repeat(STUCK_PREFETCHES) { script.enqueuePrefetch(ready = false) }
        script.enqueueCreate(startTimeMs = PLAYER_TIME_MS, ready = true, sessionId = "fresh")
        script.enqueuePosition(PLAYER_TIME_MS, sessionId = "fresh")
        script.enqueuePrefetch(ready = true, sessionId = "fresh")
        script.enqueueWindow(PLAYER_TIME_MS, endOfStream = false, segmentStartMs = PLAYER_TIME_MS, sessionId = "fresh")

        val session = refresh()

        assertEquals("fresh", session.sessionId)
        assertEquals(PLAYER_TIME_MS, session.startTimeMs)
        val paths = script.requestPaths(1 + STUCK_PREFETCHES + 4)
        assertEquals("/api/sabr/playback/video", paths[1 + STUCK_PREFETCHES])
        assertEquals("/api/sabr/playback/fresh/segments", paths.last())
    }

    @Test
    fun aFreshSessionThatStallsAgainIsReported() = runBlocking {
        script.enqueuePosition(PLAYER_TIME_MS)
        repeat(STUCK_PREFETCHES) { script.enqueuePrefetch(ready = false) }
        script.enqueueCreate(startTimeMs = PLAYER_TIME_MS, ready = true, sessionId = "fresh")
        script.enqueuePosition(PLAYER_TIME_MS, sessionId = "fresh")
        repeat(STUCK_PREFETCHES) { script.enqueuePrefetch(ready = false, sessionId = "fresh") }

        val failure = runCatching { refresh() }.exceptionOrNull()

        assertTrue(failure is SabrControlException)
        assertEquals("youtube_sabr_preparation_timeout", (failure as SabrControlException).failureCode)
        assertEquals(2 + 2 * STUCK_PREFETCHES + 1, script.server.requestCount)
    }

    private suspend fun refresh() = SabrPlaybackSessionPreparer(
        pause = {},
        maxWindowPolls = 10,
        stuckRecoveryAttempts = STUCK_PREFETCHES - 1,
    ).refresh(script.api, script.baseUrl, script.target(), script.binding()) {
        SabrPlaybackSnapshot(playerTimeMs = PLAYER_TIME_MS, bufferedRanges = emptyList())
    }

    private companion object {
        const val PLAYER_TIME_MS = 37_000L
        const val STUCK_PREFETCHES = 3
    }
}
