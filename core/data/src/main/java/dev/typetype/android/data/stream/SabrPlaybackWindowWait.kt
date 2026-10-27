package dev.typetype.android.data.stream

import dev.typetype.android.data.network.PlaybackNetworkObserver
import dev.typetype.android.data.network.TypeTypeMediaApi
import dev.typetype.android.data.network.dto.SabrPlaybackPositionRequestDto
import dev.typetype.android.data.network.dto.SabrPlaybackResponse
import dev.typetype.android.data.network.dto.SabrPlaybackWindowRequestDto
import dev.typetype.android.domain.stream.SabrPlaybackBufferedRange
import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackTarget

internal class SabrPlaybackWindowWait(
    private val pause: suspend (Long) -> Unit,
    private val network: PlaybackNetworkObserver,
    private val maxWindowPolls: Int,
    private val stuckRecoveryAttempts: Int,
) {
    suspend fun await(
        api: TypeTypeMediaApi,
        baseUrl: String,
        target: SabrPlaybackTarget,
        control: SabrPlaybackResponse,
        bufferedRanges: List<SabrPlaybackBufferedRange>,
    ): SabrPlaybackSession = await(api, baseUrl, target, control) {
        control.windowRequest(
            ranges = bufferedRanges,
            audioOnly = target.audioOnly,
            isLive = target.isLive,
        )
    }

    suspend fun await(
        api: TypeTypeMediaApi,
        baseUrl: String,
        target: SabrPlaybackTarget,
        control: SabrPlaybackResponse,
        request: () -> SabrPlaybackWindowRequestDto,
    ): SabrPlaybackSession {
        updatePosition(api, baseUrl, control, request())
        var previousEdgeMs: Long? = null
        var stagnantAttempts = 0
        repeat(maxWindowPolls) {
            val prefetch = transientPlaybackRequest(pause, network) {
                api.prefetchSabrPlayback(control.sessionId, request())
            }
            prefetch.requireWindowEndpoint(baseUrl, control.sessionId, "prefetch")
            if (!prefetch.isSuccessful) throw sabrSessionResponseException(prefetch)
            val pending = prefetch.body()
                ?.requireWindowIdentity(control)
                ?: sabrContractMismatch("SABR returned an empty prefetch response")
            pending.throwTerminalFailure()
            if (pending.ready) {
                val segments = transientPlaybackRequest(pause, network) {
                    api.sabrPlaybackSegments(control.sessionId, request())
                }
                segments.requireWindowEndpoint(baseUrl, control.sessionId, "segments")
                if (!segments.isSuccessful) throw sabrSessionResponseException(segments)
                val window = segments.body()
                    ?.requireWindowIdentity(control)
                    ?: sabrContractMismatch("SABR returned an empty segment window")
                window.throwTerminalFailure()
                if (window.ready) return window.requireWindowResponse(baseUrl, target, control)
                stagnantAttempts = window.stagnantAttempts(previousEdgeMs, stagnantAttempts)
                previousEdgeMs = window.bufferedEdgeMs
                pause(window.retryDelay(stagnantAttempts))
            } else {
                stagnantAttempts = pending.stagnantAttempts(previousEdgeMs, stagnantAttempts)
                previousEdgeMs = pending.bufferedEdgeMs
                if (stagnantAttempts >= stuckRecoveryAttempts) {
                    throw sabrPreparationFailure(
                        "SABR prefetch stuck: segment-pending",
                        SABR_PREPARATION_TIMEOUT,
                    )
                }
                pause(pending.retryDelay(stagnantAttempts))
            }
        }
        throw sabrPreparationFailure(
            "SABR playback window timed out",
            SABR_PREPARATION_TIMEOUT,
        )
    }

    suspend fun updatePosition(
        api: TypeTypeMediaApi,
        baseUrl: String,
        control: SabrPlaybackResponse,
        request: SabrPlaybackWindowRequestDto,
    ) {
        val response = transientPlaybackRequest(pause, network) {
            api.updateSabrPlaybackPosition(
                control.sessionId,
                SabrPlaybackPositionRequestDto(
                    generation = request.generation,
                    playerTimeMs = request.playerTimeMs,
                    videoItag = request.videoItag,
                    audioItag = request.audioItag,
                    audioTrackId = request.audioTrackId,
                    playbackRate = request.playbackRate,
                    bufferedRanges = request.bufferedRanges,
                    audioOnly = request.audioOnly,
                ),
            )
        }
        response.requireControlEndpoint(
            baseUrl,
            listOf("sabr", "playback", control.sessionId, "position"),
            "SABR position update left its server session endpoint",
        )
        if (!response.isSuccessful) throw sabrSessionResponseException(response)
        val body = response.body()
            ?: sabrContractMismatch("SABR returned an empty position response")
        if (body.sessionId != control.sessionId || body.generation != control.generation) {
            sabrContractMismatch("SABR position update changed its session identity")
        }
    }
}
