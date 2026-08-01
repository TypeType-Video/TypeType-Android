package dev.typetype.android.data.stream

import dev.typetype.android.data.network.AlwaysAvailablePlaybackNetworkObserver
import dev.typetype.android.data.network.PlaybackNetworkObserver
import dev.typetype.android.data.network.TypeTypeMediaApi
import dev.typetype.android.data.network.dto.SabrPlaybackPositionRequestDto
import dev.typetype.android.data.network.dto.SabrPlaybackResponse
import dev.typetype.android.data.network.dto.SabrPlaybackWindowRequestDto
import dev.typetype.android.data.network.serverResponseException
import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackBufferedRange
import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackSnapshot
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import kotlinx.coroutines.delay

internal class SabrPlaybackSessionPreparer(
    private val pause: suspend (Long) -> Unit = { delay(it) },
    private val network: PlaybackNetworkObserver = AlwaysAvailablePlaybackNetworkObserver,
    private val maxWindowPolls: Int = 60,
) {
    suspend fun prepare(
        api: TypeTypeMediaApi,
        baseUrl: String,
        target: SabrPlaybackTarget,
        startTimeMs: Long = 0L,
    ): SabrPlaybackSession = createSessionWithRecovery(api, baseUrl, target, startTimeMs)

    suspend fun prepareOnce(
        api: TypeTypeMediaApi,
        baseUrl: String,
        target: SabrPlaybackTarget,
        startTimeMs: Long,
    ): SabrPlaybackSession = createSession(api, baseUrl, target, startTimeMs)

    private suspend fun createSession(
        api: TypeTypeMediaApi,
        baseUrl: String,
        target: SabrPlaybackTarget,
        startTimeMs: Long,
    ): SabrPlaybackSession {
        val response = transientPlaybackRequest(pause, network) {
            api.createSabrPlayback(
                target.videoId,
                target.controlRequest(startTimeMs),
            )
        }
        response.requireControlEndpoint(
            baseUrl,
            listOf("sabr", "playback", target.videoId),
            "SABR session creation left its server endpoint",
        )
        if (!response.isSuccessful) throw serverResponseException(response)
        val control = response.body()
            ?.requireControlResponse(target)
            ?: sabrContractMismatch("SABR returned an empty session response")
        return waitForWindow(api, baseUrl, target, control, emptyList())
    }

    suspend fun seek(
        api: TypeTypeMediaApi,
        baseUrl: String,
        target: SabrPlaybackTarget,
        binding: SabrPlaybackBinding,
        startTimeMs: Long,
    ): SabrPlaybackSession {
        binding.requireTarget(target)
        val response = transientPlaybackRequest(pause, network) {
            api.seekSabrPlayback(
                binding.sessionId,
                target.controlRequest(startTimeMs),
            )
        }
        response.requireControlEndpoint(
            baseUrl,
            listOf("sabr", "playback", binding.sessionId, "seek"),
            "SABR seek left its server session endpoint",
        )
        if (!response.isSuccessful) throw serverResponseException(response)
        val control = response.body()
            ?.requireControlResponse(target, binding.sessionId, binding.generation)
            ?: sabrContractMismatch("SABR returned an empty seek response")
        return waitForWindow(api, baseUrl, target, control, emptyList())
    }

    suspend fun refresh(
        api: TypeTypeMediaApi,
        baseUrl: String,
        target: SabrPlaybackTarget,
        binding: SabrPlaybackBinding,
        playerTimeMs: Long,
        bufferedRanges: List<SabrPlaybackBufferedRange>,
        playbackRate: Float = 1.0f,
    ): SabrPlaybackSession = refresh(api, baseUrl, target, binding) {
        SabrPlaybackSnapshot(playerTimeMs, bufferedRanges, playbackRate)
    }

    suspend fun refresh(
        api: TypeTypeMediaApi,
        baseUrl: String,
        target: SabrPlaybackTarget,
        binding: SabrPlaybackBinding,
        snapshot: () -> SabrPlaybackSnapshot,
    ): SabrPlaybackSession {
        binding.requireTarget(target)
        val initial = snapshot()
        val control = SabrPlaybackResponse(
            sessionId = binding.sessionId,
            videoId = target.videoId,
            manifestUrl = null,
            videoItag = binding.videoItag,
            audioItag = binding.audioItag,
            audioTrackId = binding.audioTrackId,
            startTimeMs = initial.playerTimeMs.coerceAtLeast(0L),
            generation = binding.generation,
            ready = true,
            status = "ready",
        )
        return waitForWindow(api, baseUrl, target, control) {
            val current = snapshot()
            control.windowRequest(
                current.bufferedRanges,
                target.audioOnly,
                target.isLive,
                current.playerTimeMs,
                current.playbackRate,
            )
        }
    }

    private suspend fun createSessionWithRecovery(
        api: TypeTypeMediaApi,
        baseUrl: String,
        target: SabrPlaybackTarget,
        startTimeMs: Long,
    ): SabrPlaybackSession = try {
        createSession(api, baseUrl, target, startTimeMs)
    } catch (failure: SabrPlaybackRecoveryException) {
        recoverSession(api, baseUrl, target, startTimeMs, failure)
    }

    suspend fun reportPosition(
        api: TypeTypeMediaApi,
        baseUrl: String,
        target: SabrPlaybackTarget,
        binding: SabrPlaybackBinding,
        playerTimeMs: Long,
        bufferedRanges: List<SabrPlaybackBufferedRange>,
        playbackRate: Float = 1.0f,
    ) {
        binding.requireTarget(target)
        val control = binding.controlResponse(target, playerTimeMs)
        updatePosition(
            api,
            baseUrl,
            control,
            control.windowRequest(
                ranges = bufferedRanges,
                audioOnly = target.audioOnly,
                isLive = target.isLive,
                playbackRate = playbackRate,
            ),
        )
    }

    private suspend fun waitForWindow(
        api: TypeTypeMediaApi,
        baseUrl: String,
        target: SabrPlaybackTarget,
        control: SabrPlaybackResponse,
        bufferedRanges: List<SabrPlaybackBufferedRange>,
    ): SabrPlaybackSession = waitForWindow(api, baseUrl, target, control) {
        control.windowRequest(
            ranges = bufferedRanges,
            audioOnly = target.audioOnly,
            isLive = target.isLive,
        )
    }

    private suspend fun waitForWindow(
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
            if (!prefetch.isSuccessful) throw serverResponseException(prefetch)
            val pending = prefetch.body()
                ?.requireWindowIdentity(control)
                ?: sabrContractMismatch("SABR returned an empty prefetch response")
            pending.throwTerminalFailure()
            if (pending.ready) {
                val segments = transientPlaybackRequest(pause, network) {
                    api.sabrPlaybackSegments(control.sessionId, request())
                }
                segments.requireWindowEndpoint(baseUrl, control.sessionId, "segments")
                if (!segments.isSuccessful) throw serverResponseException(segments)
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
                pause(pending.retryDelay(stagnantAttempts))
            }
        }
        throw sabrPreparationFailure(
            "SABR playback window timed out",
            "youtube_sabr_preparation_timeout",
        )
    }

    private suspend fun recoverSession(
        api: TypeTypeMediaApi,
        baseUrl: String,
        originalTarget: SabrPlaybackTarget,
        startTimeMs: Long,
        initialFailure: SabrPlaybackRecoveryException,
    ): SabrPlaybackSession {
        var currentTarget = originalTarget
        var failure = initialFailure
        val attemptedVideoItags = linkedSetOf(originalTarget.videoItag)
        repeat(MAX_FRESH_SESSION_RECOVERIES) {
            currentTarget = failure.nextTarget(originalTarget, currentTarget, attemptedVideoItags)
            attemptedVideoItags += currentTarget.videoItag
            try {
                return createSession(api, baseUrl, currentTarget, startTimeMs)
            } catch (nextFailure: SabrPlaybackRecoveryException) {
                failure = nextFailure
            }
        }
        throw failure
    }

    private suspend fun updatePosition(
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
        if (!response.isSuccessful) throw serverResponseException(response)
        val body = response.body()
            ?: sabrContractMismatch("SABR returned an empty position response")
        if (body.sessionId != control.sessionId || body.generation != control.generation) {
            sabrContractMismatch("SABR position update changed its session identity")
        }
    }

}

private const val MAX_FRESH_SESSION_RECOVERIES = 2

private fun SabrPlaybackRecoveryException.nextTarget(
    original: SabrPlaybackTarget,
    current: SabrPlaybackTarget,
    attemptedVideoItags: Set<Int>,
): SabrPlaybackTarget = when (action) {
    "retry_fresh_session" -> current
    "retry_fresh_session_lower_video_itag" -> {
        val nextItag = retryVideoItags.firstOrNull {
            it in original.recoveryVideoItags && it !in attemptedVideoItags
        } ?: throw this
        original.copy(videoItag = nextItag)
    }
    else -> throw this
}
