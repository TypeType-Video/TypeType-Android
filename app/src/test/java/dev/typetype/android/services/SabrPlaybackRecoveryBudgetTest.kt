package dev.typetype.android.services

import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackRepository
import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import dev.typetype.android.domain.stream.StreamRequestScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SabrPlaybackRecoveryBudgetTest {
    @Test
    fun `bounded recovery retries a failed fresh session within its budget`() = runBlocking {
        val state = state()
        val failure = IllegalStateException("probe failed")
        var requests = 0
        val repository = object : SabrPlaybackRepository {
            override suspend fun prepare(target: SabrPlaybackTarget) = error("Not used")

            override suspend fun recoverOnce(
                target: SabrPlaybackTarget,
                startTimeMs: Long,
            ): Result<SabrPlaybackSession> {
                requests++
                return if (requests == 1) {
                    Result.failure(failure)
                } else {
                    Result.success(session().copy(sessionId = "fresh"))
                }
            }

            override suspend fun seek(
                target: SabrPlaybackTarget,
                binding: SabrPlaybackBinding,
                startTimeMs: Long,
            ) = error("Not used")
        }
        var attempts = 0
        var appliedSessionId: String? = null
        val coordinator = SabrPlaybackSeekCoordinator(repository, this, { state }) {
                session, _, _ ->
            appliedSessionId = session.sessionId
        }

        coordinator.recoverBounded(
            state = state,
            target = state.target,
            positionMs = 67_000,
            initialFailure = IllegalStateException("initial"),
            takeAttempt = { ++attempts <= 2 },
        ).join()

        assertEquals(2, requests)
        assertEquals("fresh", appliedSessionId)
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

    private fun session() = SabrPlaybackSession(
        sessionId = "session",
        manifestUrl = "https://instance.example/api/sabr/playback/session/manifest",
        generation = 0,
        videoItag = 137,
        audioItag = 140,
        audioTrackId = "en.0",
    )
}
