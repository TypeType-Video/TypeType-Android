package dev.typetype.android.services

import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackBufferedRange
import dev.typetype.android.domain.stream.SabrPlaybackRepository
import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackSnapshot
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import dev.typetype.android.domain.stream.SabrPlaybackWindowTrack
import dev.typetype.android.domain.stream.accept
import dev.typetype.android.domain.stream.binding
import dev.typetype.player.PlaybackBufferedRange
import dev.typetype.player.PlaybackLoadCancellation
import dev.typetype.player.PlaybackLiveWindow
import dev.typetype.player.PlaybackSegment
import dev.typetype.player.PlaybackTrack
import dev.typetype.player.PlaybackTrackKind
import dev.typetype.player.PlaybackWindow
import dev.typetype.player.PlaybackWindowLoader
import dev.typetype.player.PlaybackWindowRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class SabrPlaybackWindowLoader(
    private val repository: SabrPlaybackRepository,
    private val target: SabrPlaybackTarget,
    private val transportState: SabrPlaybackTransportState,
    private val recoveryDispatcher: SabrPlaybackRecoveryDispatcher,
    private val playbackRate: () -> Float,
) : PlaybackWindowLoader {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun load(
        request: () -> PlaybackWindowRequest,
        callback: (Result<PlaybackWindow>) -> Unit,
    ): PlaybackLoadCancellation {
        return launchWindowRequest(callback) {
            val activeBinding = transportState.currentBinding()
            repository.refresh(
                target = target,
                binding = activeBinding,
            ) {
                val current = request()
                SabrPlaybackSnapshot(
                    playerTimeMs = current.positionUs.toMilliseconds(),
                    bufferedRanges = current.bufferedRanges.mapNotNull(::toDomainRange),
                    playbackRate = playbackRate(),
                )
            }
        }
    }

    override fun seek(
        positionUs: Long,
        callback: (Result<PlaybackWindow>) -> Unit,
    ): PlaybackLoadCancellation {
        return launchWindowRequest(callback) {
            repository.seek(
                target = target,
                binding = transportState.currentBinding(),
                startTimeMs = positionUs.toMilliseconds(),
            )
        }
    }

    override fun release() {
        scope.cancel()
    }

    private fun toDomainRange(range: PlaybackBufferedRange): SabrPlaybackBufferedRange? {
        val itag = range.trackId.toIntOrNull()?.takeIf { it > 0 } ?: return null
        return SabrPlaybackBufferedRange(
            itag = itag,
            startMs = range.startPositionUs.toMilliseconds(),
            endMs = range.endPositionUs.toMilliseconds(),
        )
    }

    private fun launchWindowRequest(
        callback: (Result<PlaybackWindow>) -> Unit,
        request: suspend () -> Result<SabrPlaybackSession>,
    ): PlaybackLoadCancellation {
        val job = scope.launch {
            val response = request()
            val recovery = response.exceptionOrNull()?.sabrPlaybackRecoveryFailure()
            if (recovery != null) {
                recoveryDispatcher.request(
                    sessionId = transportState.currentBinding().sessionId,
                    failure = recovery,
                ) { result ->
                    result.exceptionOrNull()?.let {
                        callback(Result.failure(SabrPlaybackRecoveryExhaustedException(it)))
                    }
                }
                return@launch
            }
            val result = response.mapCatching { session ->
                if (session.videoItag != transportState.currentBinding().videoItag) {
                    target.accept(session)
                    throw SabrPlaybackSessionReplacementRequiredException(session)
                }
                require(
                    session.audioItag == target.audioItag &&
                        session.audioTrackId == target.audioTrackId,
                ) {
                    "SABR changed the audio track during active playback"
                }
                transportState.accept(session)
                session.toPlayerWindow()
            }
            callback(result)
        }
        return PlaybackLoadCancellation(job::cancel)
    }
}

internal fun SabrPlaybackSession.toPlayerWindow(): PlaybackWindow {
    val audio = requireNotNull(audioWindow) { "SABR omitted its validated audio window" }
    return PlaybackWindow(
        generation = generation,
        durationUs = durationMs.toMicroseconds(),
        startPositionUs = startTimeMs.toMicroseconds(),
        endOfStream = endOfStream,
        audio = audio.toPlayerTrack(PlaybackTrackKind.Audio),
        video = videoWindow?.toPlayerTrack(PlaybackTrackKind.Video),
        live = live?.let {
            PlaybackLiveWindow(
                active = it.active,
                postLiveDvr = it.postLiveDvr,
                headPositionUs = it.headTimeMs.toMicroseconds(),
                seekableStartPositionUs = it.seekableStartMs.toMicroseconds(),
                seekableEndPositionUs = it.seekableEndMs.toMicroseconds(),
                atLiveEdge = it.atLiveEdge,
                targetLatencyUs = it.targetLatencyMs.toMicroseconds(),
            )
        },
    )
}

private fun SabrPlaybackWindowTrack.toPlayerTrack(kind: PlaybackTrackKind) = PlaybackTrack(
    kind = kind,
    id = itag.toString(),
    mimeType = mimeType,
    initializationUrl = initializationUrl,
    segments = segments.map {
        PlaybackSegment(
            url = it.url,
            startPositionUs = it.startMs.toMicroseconds(),
            durationUs = it.durationMs.toMicroseconds(),
        )
    },
)

private fun Long.toMicroseconds(): Long = Math.multiplyExact(this, 1_000L)

private fun Long.toMilliseconds(): Long = Math.floorDiv(this, 1_000L)
