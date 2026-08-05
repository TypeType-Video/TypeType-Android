package dev.typetype.android.services

import dev.typetype.android.data.network.PlaybackNetworkState
import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackRepository
import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import dev.typetype.android.domain.stream.StreamRequestScope
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackResilienceLoadHarnessTest {
    @Test
    fun `rapid seek bursts finish on the latest requested position`() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val state = state()
        val requestedCount = AtomicInteger()
        val appliedPositions = Collections.synchronizedList(mutableListOf<Long>())
        val repository = object : SabrPlaybackRepository {
            override suspend fun prepare(target: SabrPlaybackTarget) = error("Not used")

            override suspend fun seek(
                target: SabrPlaybackTarget,
                binding: SabrPlaybackBinding,
                startTimeMs: Long,
            ): Result<SabrPlaybackSession> {
                requestedCount.incrementAndGet()
                yield()
                return Result.success(session())
            }
        }
        val coordinator = SabrPlaybackSeekCoordinator(repository, scope, { state }) {
                _, _, positionMs ->
            appliedPositions += positionMs
        }
        val positions = buildList(SEEK_COUNT) {
            val random = Random(SEEK_SEED)
            repeat(SEEK_COUNT) {
                add(random.nextLong(0L, VIDEO_DURATION_MS))
            }
        }

        withTimeout(LOAD_TIMEOUT_MS) {
            positions.map { coordinator.seek(state, it) }.joinAll()
        }

        assertTrue(requestedCount.get() in 1..SEEK_COUNT)
        assertEquals(positions.last(), appliedPositions.last())
        coordinator.cancel()
        scope.cancel()
    }

    @Test
    fun `repeated route changes never overlap recovery`() {
        val gate = PlaybackNetworkRecoveryGate()
        gate.transition(MEDIA_ID)

        repeat(NETWORK_CYCLE_COUNT) { cycle ->
            val lostGeneration = cycle.toLong() * 2L
            val restoredGeneration = lostGeneration + 1L

            assertEquals(
                PlaybackNetworkRecoveryAction.Wait,
                gate.networkChanged(PlaybackNetworkState(false, lostGeneration)),
            )
            assertEquals(
                PlaybackNetworkRecoveryAction.RetryAfter(0L),
                gate.networkChanged(PlaybackNetworkState(true, restoredGeneration)),
            )
            assertTrue(gate.startRecovery(MEDIA_ID))
            assertFalse(gate.startRecovery(MEDIA_ID))
            gate.recovered()
            assertFalse(gate.isPending(MEDIA_ID))
        }
    }

    private fun state() = SabrPlaybackSeekState(
        mediaId = MEDIA_ID,
        target = target(),
        binding = SabrPlaybackBinding("session", 3L, 137, 140, "en.0"),
    )

    private fun target() = SabrPlaybackTarget(
        videoId = "video",
        requestScope = StreamRequestScope("server", "account", "https://instance.example/api/"),
        videoItag = 137,
        audioItag = 140,
        audioTrackId = "en.0",
    )

    private fun session() = SabrPlaybackSession(
        sessionId = "session",
        manifestUrl = "https://instance.example/api/sabr/playback/session/manifest",
        generation = 3L,
        videoItag = 137,
        audioItag = 140,
        audioTrackId = "en.0",
    )

    private companion object {
        const val MEDIA_ID = "https://youtube.example/watch?v=video"
        const val SEEK_COUNT = 5_000
        const val NETWORK_CYCLE_COUNT = 20_000
        const val SEEK_SEED = 37
        const val VIDEO_DURATION_MS = 14_400_000L
        const val LOAD_TIMEOUT_MS = 15_000L
    }
}
