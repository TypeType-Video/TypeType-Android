package dev.typetype.android.services

import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackRepository
import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import dev.typetype.android.domain.stream.StreamRequestScope
import dev.typetype.android.domain.stream.sourceKey
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SabrPlaybackAudioOnlyCoordinatorTest {
    @Test
    fun `mode switch keeps the session and position while changing target mode`() = runBlocking {
        val state = state()
        var request: Triple<SabrPlaybackTarget, SabrPlaybackBinding, Long>? = null
        var appliedTarget: SabrPlaybackTarget? = null
        var completion: Result<Unit>? = null
        val repository = object : SabrPlaybackRepository {
            override suspend fun prepare(target: SabrPlaybackTarget) = error("Not used")

            override suspend fun seek(
                target: SabrPlaybackTarget,
                binding: SabrPlaybackBinding,
                startTimeMs: Long,
            ): Result<SabrPlaybackSession> {
                request = Triple(target, binding, startTimeMs)
                return Result.success(session())
            }
        }
        val coordinator = SabrPlaybackSeekCoordinator(repository, this, { state }) {
                _, target, _ ->
            appliedTarget = target
        }

        coordinator.switchAudioOnly(state, true, 72_000) {
            completion = it
        }.join()

        assertEquals(state.binding, request?.second)
        assertEquals(72_000L, request?.third)
        assertTrue(requireNotNull(request).first.audioOnly)
        assertTrue(requireNotNull(appliedTarget).audioOnly)
        assertTrue(requireNotNull(completion).isSuccess)
    }

    @Test
    fun `audio-only mode is part of playback source identity`() {
        val target = state().target

        assertTrue(target.sourceKey != target.copy(audioOnly = true).sourceKey)
    }

    private fun state() = SabrPlaybackSeekState(
        mediaId = "media",
        target = SabrPlaybackTarget(
            videoId = "video",
            requestScope = StreamRequestScope(
                "server",
                "account",
                "https://instance.example/api/",
            ),
            videoItag = 137,
            audioItag = 140,
            audioTrackId = null,
        ),
        binding = SabrPlaybackBinding("session", 3, 137, 140, null),
    )

    private fun session() = SabrPlaybackSession(
        sessionId = "session",
        manifestUrl = "https://instance.example/api/sabr/playback/session/manifest",
        generation = 4,
        videoItag = 137,
        audioItag = 140,
        audioTrackId = null,
    )
}
