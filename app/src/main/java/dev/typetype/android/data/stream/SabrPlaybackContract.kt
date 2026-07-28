package dev.typetype.android.data.stream

import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.data.network.dto.SabrPlaybackResponse
import dev.typetype.android.data.network.dto.SabrLivePlaybackDto
import dev.typetype.android.data.network.dto.SabrPlaybackWindowResponseDto
import dev.typetype.android.data.network.dto.SabrPlaybackWindowTrackDto
import dev.typetype.android.domain.stream.SabrLivePlaybackWindow
import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import dev.typetype.android.domain.stream.SabrPlaybackWindowSegment
import dev.typetype.android.domain.stream.SabrPlaybackWindowTrack
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal fun SabrPlaybackResponse.requireControlResponse(
    target: SabrPlaybackTarget,
    expectedSessionId: String? = null,
    previousGeneration: Long? = null,
): SabrPlaybackResponse {
    val validPending = !ready && retryAfterMs in MIN_RETRY_MS..MAX_RETRY_MS
    val validReady = ready && retryAfterMs == null
    if (
        sessionId.isBlank() || videoId != target.videoId ||
        videoItag != target.videoItag || audioItag != target.audioItag ||
        audioTrackId != target.audioTrackId || startTimeMs < 0L || generation < 0L ||
        expectedSessionId?.let { sessionId != it } == true ||
        previousGeneration?.let { generation <= it } == true ||
        (!validPending && !validReady) ||
        (target.isLive != (live != null))
    ) {
        sabrContractMismatch("SABR returned a different session or format tuple")
    }
    live?.requireControlLive()
    return this
}

internal fun SabrPlaybackWindowResponseDto.requireWindowResponse(
    baseUrl: String,
    target: SabrPlaybackTarget,
    control: SabrPlaybackResponse,
): SabrPlaybackSession {
    if (sessionId != control.sessionId || generation != control.generation || !ready) {
        sabrContractMismatch("SABR returned a mismatched playback window")
    }
    terminalError?.takeIf { it.isNotBlank() }?.let {
        throw SabrControlException(
            message = it,
            failureCode = if (recoveryAction.isNullOrBlank()) {
                "youtube_sabr_window_failed"
            } else {
                "youtube_sabr_recovery_required"
            },
        )
    }
    val audioTrack = audio ?: sabrContractMismatch("SABR omitted its audio window")
    val audioWindow = audioTrack.requireTrack(baseUrl, control, target.audioItag, "audio")
    val videoWindow = when {
        target.audioOnly && video != null ->
            sabrContractMismatch("SABR returned video in audio-only mode")
        target.audioOnly -> null
        else -> (video ?: sabrContractMismatch("SABR omitted its video window"))
            .requireTrack(baseUrl, control, target.videoItag, "video")
    }
    val startMs = startTimeMs ?: control.startTimeMs
    val windowEndMs = videoWindow?.let { minOf(audioWindow.endMs, it.endMs) }
        ?: audioWindow.endMs
    val liveWindow = live.requireReadyLive(target)
    val presentationDurationMs = if (liveWindow?.active == true) {
        maxOf(durationMs ?: 0L, liveWindow.headTimeMs, windowEndMs)
    } else {
        durationMs
            ?.takeIf { it > 0L }
            ?: sabrContractMismatch("SABR omitted its presentation duration")
    }
    val exceedsPresentation = liveWindow?.active != true &&
        windowEndMs > presentationDurationMs + END_TOLERANCE_MS
    if (startMs < 0L || windowEndMs <= startMs || exceedsPresentation) {
        sabrContractMismatch("SABR returned an invalid bounded playback window")
    }
    val manifestUrl = resolveSabrPlaybackManifestUrl(
        baseUrl,
        control.manifestUrl,
        control.sessionId,
    ) ?: sabrContractMismatch("SABR returned a mismatched manifest URL")
    return SabrPlaybackSession(
        sessionId = control.sessionId,
        manifestUrl = manifestUrl,
        generation = control.generation,
        videoItag = control.videoItag,
        audioItag = control.audioItag,
        audioTrackId = control.audioTrackId,
        startTimeMs = startMs,
        windowEndMs = windowEndMs,
        durationMs = presentationDurationMs,
        endOfStream = endOfStream == true,
        audioWindow = audioWindow,
        videoWindow = videoWindow,
        live = liveWindow,
    )
}

private fun SabrLivePlaybackDto?.requireReadyLive(
    target: SabrPlaybackTarget,
): SabrLivePlaybackWindow? {
    if (!target.isLive) {
        if (this != null) sabrContractMismatch("SABR changed a VOD into a live presentation")
        return null
    }
    val value = this ?: sabrContractMismatch("SABR omitted its live playback window")
    value.requireControlLive()
    if (
        value.seekableEndMs <= value.seekableStartMs ||
        value.headTimeMs < value.seekableEndMs
    ) {
        sabrContractMismatch("SABR returned an invalid live playback window")
    }
    return SabrLivePlaybackWindow(
        active = value.active,
        postLiveDvr = value.postLiveDvr,
        headSequence = value.headSequence,
        headTimeMs = value.headTimeMs,
        seekableStartMs = value.seekableStartMs,
        seekableEndMs = value.seekableEndMs,
        atLiveEdge = value.atLiveEdge,
        targetLatencyMs = value.targetLatencyMs,
    )
}

private fun SabrLivePlaybackDto.requireControlLive() {
    if (
        active == postLiveDvr || headSequence < 0L || headTimeMs < 0L ||
        seekableStartMs < 0L || seekableEndMs < seekableStartMs ||
        targetLatencyMs <= 0L
    ) {
        sabrContractMismatch("SABR returned invalid live playback metadata")
    }
}

private fun SabrPlaybackWindowTrackDto.requireTrack(
    baseUrl: String,
    control: SabrPlaybackResponse,
    expectedItag: Int,
    expectedKind: String,
): SabrPlaybackWindowTrack {
    if (!mime.substringBefore(';').trim().startsWith("$expectedKind/", ignoreCase = true)) {
        sabrContractMismatch("SABR returned an invalid $expectedKind track")
    }
    val resolvedInitializationUrl = requireMediaUrl(
        baseUrl,
        initUrl,
        control,
        expectedItag,
        initialization = true,
    )
    if (segments.isEmpty()) sabrContractMismatch("SABR returned an empty $expectedKind window")
    var previousEndMs = -1L
    val resolvedSegments = segments.map { segment ->
        if (
            segment.startMs < 0L || segment.durationMs <= 0L ||
            segment.startMs < previousEndMs &&
            previousEndMs - segment.startMs > TIMELINE_ROUNDING_TOLERANCE_MS
        ) {
            sabrContractMismatch("SABR returned an invalid $expectedKind timeline")
        }
        val resolvedUrl = requireMediaUrl(
            baseUrl,
            segment.url,
            control,
            expectedItag,
            initialization = false,
        )
        previousEndMs = try {
            Math.addExact(segment.startMs, segment.durationMs)
        } catch (_: ArithmeticException) {
            sabrContractMismatch("SABR returned an invalid $expectedKind timeline")
        }
        SabrPlaybackWindowSegment(
            url = resolvedUrl,
            startMs = segment.startMs,
            durationMs = segment.durationMs,
        )
    }
    return SabrPlaybackWindowTrack(
        itag = expectedItag,
        mimeType = mime,
        initializationUrl = resolvedInitializationUrl,
        segments = resolvedSegments,
    )
}

private fun requireMediaUrl(
    baseUrl: String,
    value: String,
    control: SabrPlaybackResponse,
    expectedItag: Int,
    initialization: Boolean,
): String {
    val resolved = resolveServerUrl(baseUrl, value)?.toHttpUrlOrNull()
        ?: sabrContractMismatch("SABR media left its server origin")
    val suffix = if (initialization) {
        listOf("sabr", "playback", control.sessionId, expectedItag.toString(), "init")
    } else {
        val sequence = resolved.pathSegments.lastOrNull()?.toLongOrNull()
        if (sequence == null || sequence < 0L) sabrContractMismatch("SABR returned an invalid segment")
        listOf("sabr", "playback", control.sessionId, expectedItag.toString(), "segment", sequence.toString())
    }
    if (resolved.pathSegments.takeLast(suffix.size) != suffix) {
        sabrContractMismatch("SABR returned a mismatched media route")
    }
    resolved.requireGeneration(control.generation)
    return resolved.toString()
}

private fun HttpUrl.requireGeneration(expected: Long) {
    val names = queryParameterNames
    if (names.any { it !in ALLOWED_MEDIA_QUERY_FIELDS }) {
        sabrContractMismatch("SABR returned unsupported media query parameters")
    }
    if (queryParameterValues("generation") != listOf(expected.toString())) {
        sabrContractMismatch("SABR returned a mismatched media generation")
    }
}

internal fun sabrPreparationFailure(
    message: String,
    code: String = "youtube_sabr_preparation_failed",
): SabrControlException = SabrControlException(message, code)

internal class SabrControlException(
    message: String,
    override val failureCode: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause), CodedFailure {
    override val requestId: String? = null
    override val statusCode: Int? = null
}

internal class SabrPlaybackRecoveryException(
    message: String,
    val action: String?,
    val retryVideoItags: List<Int>,
) : IllegalStateException(message), CodedFailure {
    override val failureCode: String =
        if (action.isNullOrBlank()) "youtube_sabr_window_failed" else "youtube_sabr_recovery_required"
    override val requestId: String? = null
    override val statusCode: Int? = null
}

private val ALLOWED_MEDIA_QUERY_FIELDS = setOf("session", "generation")
private const val MIN_RETRY_MS = 250L
private const val MAX_RETRY_MS = 2_000L
private const val END_TOLERANCE_MS = 2_000L
private const val TIMELINE_ROUNDING_TOLERANCE_MS = 1L
