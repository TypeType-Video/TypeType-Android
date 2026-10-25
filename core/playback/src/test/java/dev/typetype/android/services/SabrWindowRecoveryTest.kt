package dev.typetype.android.services

import dev.typetype.android.data.network.PlaybackNetworkObserver
import dev.typetype.android.data.network.PlaybackNetworkState
import dev.typetype.android.data.network.ServerError
import dev.typetype.android.data.network.ServerResponseException
import dev.typetype.android.data.stream.SABR_PREPARATION_TIMEOUT
import dev.typetype.android.data.stream.SabrControlException
import dev.typetype.android.data.stream.SabrPlaybackRecoveryException
import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackRepository
import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import dev.typetype.android.domain.stream.StreamRequestScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SabrWindowRecoveryTest {
    private var nowMs = 0L
    private val pauses = mutableListOf<Long>()
    private val freshStarts = mutableListOf<Long>()
    private var networkAvailable = true
    private var networkReturns = true

    private val repository = object : SabrPlaybackRepository {
        override suspend fun prepare(target: SabrPlaybackTarget) = error("unused")

        override suspend fun seek(target: SabrPlaybackTarget, binding: SabrPlaybackBinding, startTimeMs: Long) =
            error("unused")

        override suspend fun recoverOnce(target: SabrPlaybackTarget, startTimeMs: Long): Result<SabrPlaybackSession> {
            freshStarts += startTimeMs
            return Result.success(session("fresh"))
        }
    }

    private val network = object : PlaybackNetworkObserver {
        override fun snapshot() = PlaybackNetworkState(isAvailable = networkAvailable, generation = 1)

        override suspend fun awaitAvailableAfter(generation: Long, timeoutMs: Long): Boolean {
            networkAvailable = networkReturns
            return networkReturns
        }
    }

    private val recovery = SabrWindowRecovery(
        repository = repository,
        target = TARGET,
        network = network,
        clock = { nowMs },
        pause = { pauses += it; nowMs += it },
    )

    @Test
    fun aBusyServerIsAskedAgainWithGrowingPauses() = runBlocking {
        val replies = replies(listOf(server(503), server(503), Result.success(session("same"))))

        val result = recovery.run({ 50_000 }) { replies.removeFirst() }

        assertEquals("same", result.getOrThrow().sessionId)
        assertEquals(listOf(1_000L, 2_000L), pauses)
    }

    @Test
    fun aStalledOrLostSessionIsReplacedAtThePlayerPosition() = runBlocking {
        val stuck = Result.failure<SabrPlaybackSession>(SabrControlException("stuck", SABR_PREPARATION_TIMEOUT))

        val result = recovery.run({ 50_000 }) { stuck }

        assertEquals("fresh", result.getOrThrow().sessionId)
        assertEquals(listOf(50_000L), freshStarts)
    }

    @Test
    fun aQualityChangeOrAFatalFailureIsLeftToTheService() = runBlocking {
        val lower = SabrPlaybackRecoveryException("lower", "retry_fresh_session_lower_video_itag", listOf(136))

        val rebuild = recovery.run({ 0 }) { Result.failure(lower) }
        val fatal = recovery.run({ 0 }) { server(401) }

        assertTrue(rebuild.exceptionOrNull() === lower)
        assertTrue(fatal.isFailure)
        assertEquals(emptyList<Long>(), pauses)
    }

    @Test
    fun theFailureIsReportedOnceTheRecoveryWindowIsOver() = runBlocking {
        var requests = 0

        val result = recovery.run({ 0 }) { requests++; server(503) }

        assertTrue(result.isFailure)
        assertTrue(nowMs >= PlaybackRecoveryPolicy().windowMs)
        assertEquals(pauses.size + 1, requests)
    }

    @Test
    fun withoutANetworkItWaitsForOneAndGivesUpIfNoneComes() = runBlocking {
        networkAvailable = false
        val replies = replies(listOf(network(), Result.success(session("same"))))

        val result = recovery.run({ 0 }) { replies.removeFirst() }

        assertEquals("same", result.getOrThrow().sessionId)
        networkAvailable = false
        networkReturns = false
        assertTrue(recovery.run({ 0 }) { network() }.isFailure)
        assertEquals(emptyList<Long>(), pauses)
    }

    private fun replies(results: List<Result<SabrPlaybackSession>>) = ArrayDeque(results)

    private fun server(status: Int) =
        Result.failure<SabrPlaybackSession>(ServerResponseException(ServerError("failed", null, status)))

    private fun network() = Result.failure<SabrPlaybackSession>(java.net.SocketTimeoutException("read"))

    private fun session(id: String) = SabrPlaybackSession(
        sessionId = id,
        manifestUrl = "https://example.test/manifest",
        generation = 0,
        videoItag = 137,
        audioItag = 140,
        audioTrackId = "en.0",
    )

    private companion object {
        val TARGET = SabrPlaybackTarget(
            videoId = "video",
            requestScope = StreamRequestScope("server", "account", "https://example.test/api/"),
            videoItag = 137,
            audioItag = 140,
            audioTrackId = "en.0",
        )
    }
}
