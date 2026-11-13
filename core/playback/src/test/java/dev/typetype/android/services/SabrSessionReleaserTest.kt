package dev.typetype.android.services

import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackRepository
import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import dev.typetype.android.playback.SabrFixtures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class SabrSessionReleaserTest {
    private val released = mutableListOf<String>()
    private val repository = object : SabrPlaybackRepository {
        override suspend fun prepare(target: SabrPlaybackTarget): Result<SabrPlaybackSession> = error("unused")

        override suspend fun seek(
            target: SabrPlaybackTarget,
            binding: SabrPlaybackBinding,
            startTimeMs: Long,
        ): Result<SabrPlaybackSession> = error("unused")

        override suspend fun release(target: SabrPlaybackTarget, sessionId: String): Result<Unit> {
            released += sessionId
            return Result.success(Unit)
        }
    }
    private val releaser = SabrSessionReleaser(repository, CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun `moving to another video releases the previous session`() {
        releaser.hold(state("first"))
        releaser.hold(state("second"))

        assertEquals(listOf("first"), released)
    }

    @Test
    fun `a new generation of the same session is not released`() {
        releaser.hold(state("same", generation = 0L))
        releaser.hold(state("same", generation = 3L))

        assertEquals(emptyList<String>(), released)
    }

    @Test
    fun `leaving SABR playback or closing releases the held session once`() {
        releaser.hold(state("first"))
        releaser.hold(null)
        releaser.hold(state("second"))
        releaser.releaseAll()
        releaser.releaseAll()

        assertEquals(listOf("first", "second"), released)
    }

    private fun state(sessionId: String, generation: Long = 0L) = SabrPlaybackSeekState(
        mediaId = sessionId,
        target = SabrFixtures.target(),
        binding = SabrFixtures.binding().copy(sessionId = sessionId, generation = generation),
    )
}
