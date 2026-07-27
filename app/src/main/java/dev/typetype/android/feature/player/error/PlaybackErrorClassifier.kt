package dev.typetype.android.feature.player.error

import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import dev.typetype.android.core.error.CodedFailure

enum class PlaybackFailureKind {
    AuthenticationExpired,
    PlaybackSessionExpired,
    PlaybackGenerationChanged,
    YouTubeSessionRequired,
    SabrServerContract,
    SabrRecoveryExhausted,
    MediaDelivery,
    Network,
    UnsupportedFormat,
    CleartextBlocked,
    BehindLiveWindow,
    Generic,
}

fun classifyPlaybackError(error: PlaybackException): PlaybackFailureKind =
    classifyPlaybackCause(error, error.errorCode)

@OptIn(UnstableApi::class)
fun playbackRequestId(error: PlaybackException): String? =
    requestIdFromHeaders(error.findHttpFailure()?.headerFields.orEmpty())

internal fun classifyPlaybackCause(error: Throwable, errorCode: Int): PlaybackFailureKind {
    if (error.hasFailureCode(SABR_CONTRACT_FAILURE_CODE)) {
        return PlaybackFailureKind.SabrServerContract
    }
    if (error.hasFailureCode(SABR_RECOVERY_EXHAUSTED_FAILURE_CODE)) {
        return PlaybackFailureKind.SabrRecoveryExhausted
    }
    error.findHttpFailure()?.let { failure ->
        val body = failure.responseBody
            .take(MAX_RESPONSE_BODY_BYTES)
            .toByteArray()
            .toString(Charsets.UTF_8)
        return classifyHttpPlaybackFailure(body, failure.responseCode)
    }
    return classifyPlaybackErrorCode(errorCode)
}

private fun Throwable.hasFailureCode(code: String): Boolean {
    var current: Throwable? = this
    repeat(MAX_CAUSE_DEPTH) {
        if ((current as? CodedFailure)?.failureCode == code) return true
        current = current?.cause?.takeUnless { it === current }
    }
    return false
}

internal fun classifyHttpPlaybackFailure(
    responseBody: String?,
    statusCode: Int? = null,
): PlaybackFailureKind =
    when {
        statusCode == 401 -> PlaybackFailureKind.AuthenticationExpired
        statusCode == 404 || statusCode == 410 -> PlaybackFailureKind.PlaybackSessionExpired
        statusCode == 409 -> PlaybackFailureKind.PlaybackGenerationChanged
        isYouTubeSessionRequiredMessage(responseBody) -> PlaybackFailureKind.YouTubeSessionRequired
        else -> PlaybackFailureKind.MediaDelivery
    }

internal fun requestIdFromHeaders(headers: Map<String, List<String>>): String? =
    headers.entries
        .firstOrNull { it.key.equals(REQUEST_ID_HEADER, ignoreCase = true) }
        ?.value
        ?.firstOrNull()
        ?.takeIf(REQUEST_ID_PATTERN::matches)

internal fun classifyPlaybackErrorCode(errorCode: Int): PlaybackFailureKind = when (errorCode) {
    PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> PlaybackFailureKind.BehindLiveWindow
    PlaybackException.ERROR_CODE_TIMEOUT,
    PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
    -> PlaybackFailureKind.Network
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> PlaybackFailureKind.MediaDelivery
    PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED -> PlaybackFailureKind.CleartextBlocked
    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
    PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
    PlaybackException.ERROR_CODE_DECODING_FAILED,
    PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
    -> PlaybackFailureKind.UnsupportedFormat
    else -> PlaybackFailureKind.Generic
}

private fun Throwable.findHttpFailure(): HttpDataSource.InvalidResponseCodeException? {
    var current: Throwable? = this
    repeat(MAX_CAUSE_DEPTH) {
        if (current is HttpDataSource.InvalidResponseCodeException) return current
        current = current?.cause?.takeUnless { it === current }
    }
    return null
}

private const val MAX_RESPONSE_BODY_BYTES = 8_192
private const val MAX_CAUSE_DEPTH = 8
private const val REQUEST_ID_HEADER = "X-Request-ID"
private const val SABR_CONTRACT_FAILURE_CODE = "youtube_sabr_contract_mismatch"
private const val SABR_RECOVERY_EXHAUSTED_FAILURE_CODE = "youtube_sabr_recovery_exhausted"
private val REQUEST_ID_PATTERN = Regex("[A-Za-z0-9._:-]{1,128}")
