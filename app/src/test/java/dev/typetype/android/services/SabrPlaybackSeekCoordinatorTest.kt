package dev.typetype.android.services

import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackRepository
import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import dev.typetype.android.domain.stream.StreamRequestScope
import dev.typetype.android.data.stream.SabrPlaybackRecoveryException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SabrPlaybackSeekCoordinatorTest {
    @Test
    fun `successful seek uses exact state and applies accepted session`() = runBlocking {
        var requested: Triple<SabrPlaybackTarget, SabrPlaybackBinding, Long>? = null
        val repository = FakeRepository { target, binding, position ->
            requested = Triple(target, binding, position)
            Result.success(session(generation = 4))
        }
        val state = state()
        var applied: Triple<SabrPlaybackSession, SabrPlaybackTarget, Long>? = null
        val coordinator = SabrPlaybackSeekCoordinator(repository, this, { state }) { session, target, position ->
            applied = Triple(session, target, position)
        }

        coordinator.seek(state, 90_000).join()

        assertEquals(Triple(state.target, state.binding, 90_000L), requested)
        assertEquals(4L, applied?.first?.generation)
        assertEquals(state.target, applied?.second)
        assertEquals(90_000L, applied?.third)
    }

    @Test
    fun `new seek rejects an older result even when cancellation is ignored`() = runBlocking {
        val firstResult = CompletableDeferred<Result<SabrPlaybackSession>>()
        var calls = 0
        val repository = FakeRepository { _, _, _ ->
            calls++
            if (calls == 1) withContext(NonCancellable) { firstResult.await() }
            else Result.success(session(generation = 5))
        }
        val state = state()
        val appliedGenerations = mutableListOf<Long>()
        val coordinator = SabrPlaybackSeekCoordinator(repository, this, { state }) { session, _, _ ->
            appliedGenerations += session.generation
        }

        val first = coordinator.seek(state, 30_000)
        yield()
        val second = coordinator.seek(state, 60_000)
        second.join()
        firstResult.complete(Result.success(session(generation = 4)))
        first.join()

        assertEquals(listOf(5L), appliedGenerations)
    }

    @Test
    fun `result is ignored after active SABR state changes`() = runBlocking {
        val result = CompletableDeferred<Result<SabrPlaybackSession>>()
        var current: SabrPlaybackSeekState? = state()
        var applied = false
        val coordinator = SabrPlaybackSeekCoordinator(
            FakeRepository { _, _, _ -> result.await() },
            this,
            { current },
        ) { _, _, _ -> applied = true }

        val job = coordinator.seek(requireNotNull(current), 90_000)
        yield()
        current = state().copy(mediaId = "another-media")
        result.complete(Result.success(session(generation = 4)))
        job.join()

        assertTrue(!applied)
    }

    @Test
    fun `failed server seek does not replace current SABR media`() = runBlocking {
        var applied = false
        val state = state()
        val coordinator = SabrPlaybackSeekCoordinator(
            FakeRepository { _, _, _ -> Result.failure(IllegalStateException("failed")) },
            this,
            { state },
        ) { _, _, _ -> applied = true }

        coordinator.seek(state, 90_000).join()

        assertTrue(!applied)
    }

    @Test
    fun `recovery creates a fresh session at the playback position`() = runBlocking {
        val state = state()
        val recoveryTarget = state.target.copy(videoItag = 136)
        var requested: Pair<SabrPlaybackTarget, Long>? = null
        var applied: Pair<SabrPlaybackSession, SabrPlaybackTarget>? = null
        var completion: Result<Unit>? = null
        val repository = object : SabrPlaybackRepository {
            override suspend fun prepare(target: SabrPlaybackTarget) =
                error("Positioned prepare expected")

            override suspend fun prepare(
                target: SabrPlaybackTarget,
                startTimeMs: Long,
            ): Result<SabrPlaybackSession> {
                requested = target to startTimeMs
                return Result.success(
                    session(generation = 0).copy(
                        sessionId = "fresh",
                        videoItag = 136,
                    ),
                )
            }

            override suspend fun seek(
                target: SabrPlaybackTarget,
                binding: SabrPlaybackBinding,
                startTimeMs: Long,
            ) = error("Fresh recovery expected")
        }
        val coordinator = SabrPlaybackSeekCoordinator(repository, this, { state }) {
                session, target, _ ->
            applied = session to target
        }

        coordinator.recover(state, recoveryTarget, 67_000) {
            completion = it
        }.join()

        assertEquals(recoveryTarget to 67_000L, requested)
        assertEquals("fresh", applied?.first?.sessionId)
        assertEquals(136, applied?.second?.videoItag)
        assertTrue(requireNotNull(completion).isSuccess)
    }

    @Test
    fun `failed recovery completes without replacing media`() = runBlocking {
        val state = state()
        val failure = IllegalStateException("failed")
        var applied = false
        var completion: Result<Unit>? = null
        val repository = object : SabrPlaybackRepository {
            override suspend fun prepare(target: SabrPlaybackTarget) =
                error("Positioned prepare expected")

            override suspend fun prepare(
                target: SabrPlaybackTarget,
                startTimeMs: Long,
            ): Result<SabrPlaybackSession> = Result.failure(failure)

            override suspend fun seek(
                target: SabrPlaybackTarget,
                binding: SabrPlaybackBinding,
                startTimeMs: Long,
            ) = error("Fresh recovery expected")
        }
        val coordinator = SabrPlaybackSeekCoordinator(repository, this, { state }) {
                _, _, _ ->
            applied = true
        }

        coordinator.recover(state, state.target, 67_000) {
            completion = it
        }.join()

        assertTrue(!applied)
        assertTrue(requireNotNull(completion).exceptionOrNull() === failure)
    }

    @Test
    fun `bounded recovery uses remaining attempts without nested preparation`() = runBlocking {
        val state = state()
        val requestedItags = mutableListOf<Int>()
        val repository = object : SabrPlaybackRepository {
            override suspend fun prepare(target: SabrPlaybackTarget) = error("Not used")

            override suspend fun recoverOnce(
                target: SabrPlaybackTarget,
                startTimeMs: Long,
            ): Result<SabrPlaybackSession> {
                requestedItags += target.videoItag
                return if (requestedItags.size == 1) {
                    Result.failure(
                        SabrPlaybackRecoveryException(
                            "lower",
                            "retry_fresh_session_lower_video_itag",
                            listOf(135),
                        ),
                    )
                } else {
                    Result.success(
                        session(0).copy(
                            sessionId = "fresh",
                            videoItag = target.videoItag,
                        ),
                    )
                }
            }

            override suspend fun seek(
                target: SabrPlaybackTarget,
                binding: SabrPlaybackBinding,
                startTimeMs: Long,
            ) = error("Not used")
        }
        val recoveryTarget = state.target.copy(
            videoItag = 136,
            recoveryVideoItags = setOf(135),
        )
        var attempts = 0
        var appliedItag: Int? = null
        val coordinator = SabrPlaybackSeekCoordinator(repository, this, { state }) {
                _, target, _ ->
            appliedItag = target.videoItag
        }

        coordinator.recoverBounded(
            state = state,
            target = recoveryTarget,
            positionMs = 67_000,
            initialFailure = IllegalStateException("initial"),
            takeAttempt = { ++attempts <= 2 },
        ).join()

        assertEquals(listOf(136, 135), requestedItags)
        assertEquals(135, appliedItag)
    }

    @Test
    fun `bounded recovery stops when no attempt remains`() = runBlocking {
        val state = state()
        var requested = false
        val repository = object : SabrPlaybackRepository {
            override suspend fun prepare(target: SabrPlaybackTarget) = error("Not used")

            override suspend fun recoverOnce(
                target: SabrPlaybackTarget,
                startTimeMs: Long,
            ): Result<SabrPlaybackSession> {
                requested = true
                return Result.success(session(0))
            }

            override suspend fun seek(
                target: SabrPlaybackTarget,
                binding: SabrPlaybackBinding,
                startTimeMs: Long,
            ) = error("Not used")
        }
        val failure = IllegalStateException("exhausted")
        var completion: Result<Unit>? = null
        val coordinator = SabrPlaybackSeekCoordinator(repository, this, { state }) {
                _, _, _ ->
            error("Must not apply")
        }

        coordinator.recoverBounded(
            state = state,
            target = state.target,
            positionMs = 67_000,
            initialFailure = failure,
            takeAttempt = { false },
        ) {
            completion = it
        }.join()

        assertTrue(!requested)
        assertTrue(requireNotNull(completion).exceptionOrNull() === failure)
    }

    private fun state() = SabrPlaybackSeekState(
        mediaId = "https://youtube.example/watch?v=video",
        target = target(),
        binding = SabrPlaybackBinding("session", 3, 137, 140, "en.0"),
    )

    private fun target() = SabrPlaybackTarget(
        videoId = "video",
        requestScope = StreamRequestScope("server", "account", "https://instance.example/api/"),
        videoItag = 137,
        audioItag = 140,
        audioTrackId = "en.0",
        recoveryVideoItags = setOf(136),
    )

    private fun session(generation: Long) = SabrPlaybackSession(
        sessionId = "session",
        manifestUrl = "https://instance.example/api/sabr/playback/session/manifest",
        generation = generation,
        videoItag = 137,
        audioItag = 140,
        audioTrackId = "en.0",
    )

    private class FakeRepository(
        private val seekResult: suspend (SabrPlaybackTarget, SabrPlaybackBinding, Long) ->
            Result<SabrPlaybackSession>,
    ) : SabrPlaybackRepository {
        override suspend fun prepare(target: SabrPlaybackTarget) =
            error("Not used")

        override suspend fun seek(
            target: SabrPlaybackTarget,
            binding: SabrPlaybackBinding,
            startTimeMs: Long,
        ) = seekResult(target, binding, startTimeMs)
    }
}
