package dev.typetype.android.data.stream

import dev.typetype.android.data.network.dto.SabrPlaybackBufferedRangeDto
import dev.typetype.android.data.network.dto.SabrPlaybackRequest
import dev.typetype.android.data.network.dto.SabrPlaybackResponse
import dev.typetype.android.data.network.dto.SabrPlaybackWindowRequestDto
import dev.typetype.android.data.network.dto.SabrPlaybackWindowResponseDto
import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackBufferedRange
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import retrofit2.Response

internal fun SabrPlaybackBinding.controlResponse(
    target: SabrPlaybackTarget,
    playerTimeMs: Long,
) = SabrPlaybackResponse(
    sessionId = sessionId,
    videoId = target.videoId,
    manifestUrl = null,
    videoItag = videoItag,
    audioItag = audioItag,
    audioTrackId = audioTrackId,
    startTimeMs = playerTimeMs.coerceAtLeast(0L),
    generation = generation,
    ready = true,
    status = "ready",
)

internal fun SabrPlaybackTarget.controlRequest(positionMs: Long) = SabrPlaybackRequest(
    videoItag = videoItag,
    audioItag = audioItag,
    audioTrackId = audioTrackId,
    startTimeMs = positionMs.coerceAtLeast(0L),
    playerTimeMs = positionMs.coerceAtLeast(0L),
    isLive = isLive,
)

internal fun SabrPlaybackResponse.windowRequest(
    ranges: List<SabrPlaybackBufferedRange>,
    playerTimeMs: Long = startTimeMs,
    playbackRate: Float = 1.0f,
) = SabrPlaybackWindowRequestDto(
    generation = generation,
    playerTimeMs = playerTimeMs.coerceAtLeast(0L),
    videoItag = videoItag,
    audioItag = audioItag,
    audioTrackId = audioTrackId,
    playbackRate = playbackRate.sanitizedPlaybackRate(),
    bufferGoalMs = playbackRate.rateAwareBufferGoalMs(),
    bufferedRanges = ranges.map {
        SabrPlaybackBufferedRangeDto(it.itag, it.startMs, it.endMs)
    },
)

internal fun SabrPlaybackWindowResponseDto.requireWindowIdentity(
    control: SabrPlaybackResponse,
): SabrPlaybackWindowResponseDto {
    if (sessionId != control.sessionId || generation != control.generation) {
        sabrContractMismatch("SABR changed its session identity while preparing a window")
    }
    return this
}

internal fun SabrPlaybackWindowResponseDto.throwTerminalFailure() {
    val message = terminalError?.takeIf { it.isNotBlank() } ?: return
    throw SabrPlaybackRecoveryException(
        message = message,
        action = recoveryAction,
        retryVideoItags = retryVideoItags,
    )
}

internal fun SabrPlaybackWindowResponseDto.stagnantAttempts(
    previousEdgeMs: Long?,
    previousAttempts: Int,
): Int = if (bufferedEdgeMs != null && bufferedEdgeMs == previousEdgeMs) {
    previousAttempts + 1
} else {
    0
}

internal fun SabrPlaybackWindowResponseDto.retryDelay(stagnantAttempts: Int): Long {
    val requested = (retryAfterMs ?: DEFAULT_RETRY_MS).coerceIn(MIN_RETRY_MS, MAX_RETRY_MS)
    val multiplier = 1L shl minOf(3, stagnantAttempts / 4)
    return (requested * multiplier).coerceAtMost(MAX_RETRY_MS)
}

internal fun SabrPlaybackBinding.requireTarget(target: SabrPlaybackTarget) {
    if (
        sessionId.isBlank() || generation < 0L || videoItag != target.videoItag ||
        audioItag != target.audioItag || audioTrackId != target.audioTrackId
    ) {
        sabrContractMismatch("SABR control does not match its active session")
    }
}

internal fun Response<*>.requireControlEndpoint(
    baseUrl: String,
    tail: List<String>,
    message: String,
) {
    if (!isExpectedServerEndpoint(baseUrl, raw().request.url.toString(), tail)) {
        sabrContractMismatch(message)
    }
}

internal fun Response<*>.requireWindowEndpoint(
    baseUrl: String,
    sessionId: String,
    action: String,
) = requireControlEndpoint(
    baseUrl,
    listOf("sabr", "playback", sessionId, action),
    "SABR $action left its server session endpoint",
)

internal val RECOVERABLE_CONTROL_STATUS_CODES = setOf(404, 409, 410)
private const val DEFAULT_RETRY_MS = 500L
private const val MIN_RETRY_MS = 250L
private const val MAX_RETRY_MS = 2_000L
private const val DEFAULT_BUFFER_GOAL_MS = 30_000L
private const val MAX_BUFFER_GOAL_MS = 60_000L
private const val MIN_PLAYBACK_RATE = 0.25f
private const val MAX_PLAYBACK_RATE = 4.0f

private fun Float.sanitizedPlaybackRate(): Float =
    takeIf { isFinite() && this in MIN_PLAYBACK_RATE..MAX_PLAYBACK_RATE } ?: 1.0f

private fun Float.rateAwareBufferGoalMs(): Long =
    (DEFAULT_BUFFER_GOAL_MS * maxOf(1.0f, sanitizedPlaybackRate()))
        .toLong()
        .coerceAtMost(MAX_BUFFER_GOAL_MS)
