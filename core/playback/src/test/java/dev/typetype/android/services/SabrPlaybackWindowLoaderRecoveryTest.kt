package dev.typetype.android.services

import dev.typetype.android.data.network.AlwaysAvailablePlaybackNetworkObserver
import dev.typetype.android.data.stream.SabrPlaybackRecoveryException
import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackBufferedRange
import dev.typetype.android.domain.stream.SabrPlaybackRepository
import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import dev.typetype.android.domain.stream.SabrPlaybackWindowSegment
import dev.typetype.android.domain.stream.SabrPlaybackWindowTrack
import dev.typetype.android.domain.stream.StreamRequestScope
import dev.typetype.android.domain.stream.binding
import dev.typetype.player.PlaybackWindowRequest
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class SabrPlaybackWindowLoaderRecoveryTest {
    @Test
    fun recoveryAfterAnInPlaceReplacementNamesTheSessionOfTheMediaItem() {
        val replies = ArrayDeque(
            listOf(
                Result.success(session("fresh")),
                Result.failure<SabrPlaybackSession>(
                    SabrPlaybackRecoveryException("lower", "retry_fresh_session_lower_video_itag", listOf(136)),
                ),
            ),
        )
        val repository = object : SabrPlaybackRepository {
            override suspend fun prepare(target: SabrPlaybackTarget) = error("unused")

            override suspend fun seek(target: SabrPlaybackTarget, binding: SabrPlaybackBinding, startTimeMs: Long) =
                error("unused")

            override suspend fun refresh(
                target: SabrPlaybackTarget,
                binding: SabrPlaybackBinding,
                playerTimeMs: Long,
                bufferedRanges: List<SabrPlaybackBufferedRange>,
                playbackRate: Float,
            ) = replies.removeFirst()
        }
        val requested = mutableListOf<String>()
        val dispatcher = SabrPlaybackRecoveryDispatcher().apply {
            setListener { sessionId, _, _ -> requested += sessionId }
        }
        val original = session("original")
        val loader = SabrPlaybackWindowLoader(
            repository = repository,
            target = TARGET,
            transportState = SabrPlaybackTransportState(original.binding),
            recoveryDispatcher = dispatcher,
            playbackRate = { 1f },
            windowRecovery = SabrWindowRecovery(repository, TARGET, AlwaysAvailablePlaybackNetworkObserver, clock = { 0L }),
            context = Dispatchers.Unconfined,
        )
        val request = { PlaybackWindowRequest(30_000_000, emptyList()) }

        loader.load(request) {}
        loader.load(request) {}

        assertEquals(listOf("original"), requested)
    }

    private fun session(id: String) = SabrPlaybackSession(
        sessionId = id,
        manifestUrl = "https://example.test/manifest",
        generation = 0,
        videoItag = 137,
        audioItag = 140,
        audioTrackId = "en.0",
        startTimeMs = 30_000,
        windowEndMs = 60_000,
        durationMs = 120_000,
        audioWindow = track(140, "audio/mp4"),
        videoWindow = track(137, "video/mp4"),
    )

    private fun track(itag: Int, mimeType: String) = SabrPlaybackWindowTrack(
        itag = itag,
        mimeType = mimeType,
        initializationUrl = "https://example.test/$itag/init",
        segments = listOf(SabrPlaybackWindowSegment("https://example.test/$itag/segment/1", 30_000, 30_000)),
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
