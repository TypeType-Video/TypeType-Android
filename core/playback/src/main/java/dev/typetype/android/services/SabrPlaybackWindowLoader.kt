package dev.typetype.android.services

import dev.typetype.android.data.stream.requestedBufferGoalMs
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
import kotlin.coroutines.CoroutineContext
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
    private val windowRecovery: SabrWindowRecovery,
    initialWindow: PlaybackWindow? = null,
    context: CoroutineContext = Dispatchers.Main.immediate,
) : PlaybackWindowLoader {
    private val scope = CoroutineScope(SupervisorJob() + context)
    private var currentWindow = initialWindow

    // The service matches recovery requests against the media item, which keeps the session it was built with. A
    // stuck session replaced in place only moves the transport binding, so requests name the original session.
    private val sourceSessionId = transportState.currentBinding().sessionId

    override fun load(
        request: () -> PlaybackWindowRequest,
        callback: (Result<PlaybackWindow>) -> Unit,
    ): PlaybackLoadCancellation {
        return launchWindowRequest(callback, { request().positionUs.toMilliseconds() }) {
            val activeBinding = transportState.currentBinding()
            var requestedBufferGoalMs = 2_500L
            repository.refresh(
                target = target,
                binding = activeBinding,
            ) {
                val current = request()
                val ranges = current.bufferedRanges.mapNotNull(::toDomainRange)
                val playerTimeMs = current.positionUs.toMilliseconds()
                val currentPlaybackRate = playbackRate()
                requestedBufferGoalMs = target.requestedBufferGoalMs(
                    ranges = ranges,
                    playerTimeMs = playerTimeMs,
                    playbackRate = currentPlaybackRate,
                )
                SabrPlaybackSnapshot(
                    playerTimeMs = playerTimeMs,
                    bufferedRanges = ranges,
                    playbackRate = currentPlaybackRate,
                )
            }.map { session -> session.copy(requestedBufferGoalMs = requestedBufferGoalMs) }
        }
    }

    override fun seek(
        positionUs: Long,
        callback: (Result<PlaybackWindow>) -> Unit,
    ): PlaybackLoadCancellation {
        return launchWindowRequest(callback, { positionUs.toMilliseconds() }) {
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
        positionMs: () -> Long,
        request: suspend () -> Result<SabrPlaybackSession>,
    ): PlaybackLoadCancellation {
        val job = scope.launch {
            val response = windowRecovery.run(positionMs, request)
            val recovery = response.exceptionOrNull()?.sabrPlaybackRecoveryFailure()
            if (recovery != null) {
                recoveryDispatcher.request(
                    sessionId = sourceSessionId,
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
                session.toPlayerWindow(currentWindow).also { currentWindow = it }
            }
            callback(result)
        }
        return PlaybackLoadCancellation(job::cancel)
    }
}

internal fun SabrPlaybackSession.toPlayerWindow(
    previous: PlaybackWindow? = null,
): PlaybackWindow {
    val audio = requireNotNull(audioWindow) { "SABR omitted its validated audio window" }
    return PlaybackWindow(
        generation = generation,
        bufferGoalUs = requestedBufferGoalMs.toMicroseconds(),
        durationUs = durationMs.toMicroseconds(),
        startPositionUs = startTimeMs.toMicroseconds(),
        endOfStream = endOfStream,
        audio = audio.toPlayerTrack(PlaybackTrackKind.Audio, previous?.audio, endOfStream),
        video = videoWindow?.toPlayerTrack(
            PlaybackTrackKind.Video,
            previous?.video,
            endOfStream,
        ),
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

private fun SabrPlaybackWindowTrack.toPlayerTrack(
    kind: PlaybackTrackKind,
    previous: PlaybackTrack?,
    endOfStream: Boolean,
): PlaybackTrack {
    if (segments.isEmpty()) {
        check(endOfStream) { "SABR returned an empty active playback window" }
        return requireNotNull(previous) { "SABR ended before providing a playback window" }
            .also {
                check(
                    it.kind == kind &&
                        it.id == itag.toString() &&
                        it.mimeType == mimeType,
                ) { "SABR changed a terminal playback track" }
            }
    }
    return PlaybackTrack(
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
}

private fun Long.toMicroseconds(): Long = Math.multiplyExact(this, 1_000L)

private fun Long.toMilliseconds(): Long = Math.floorDiv(this, 1_000L)
