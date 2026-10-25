package dev.typetype.android.services

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.data.stream.SABR_PREPARATION_TIMEOUT
import dev.typetype.android.data.stream.SabrPlaybackRecoveryException
import java.io.InterruptedIOException
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

internal enum class PlaybackRecoveryCause {
    Network,

    Server,

    Session,

    Rebuild,

    Fatal,
}

@OptIn(UnstableApi::class)
internal fun Throwable.playbackRecoveryCause(): PlaybackRecoveryCause {
    var transport = false
    var current: Throwable? = this
    var depth = 0
    while (current != null && depth < MAX_CAUSE_DEPTH) {
        when (current) {
            is SabrPlaybackRecoveryExhaustedException -> return PlaybackRecoveryCause.Session
            is SabrPlaybackSessionReplacementRequiredException -> return PlaybackRecoveryCause.Rebuild
            is SabrPlaybackRecoveryException -> return current.recoveryCause()
            is HttpDataSource.InvalidResponseCodeException -> return current.recoveryCause()
            is CodedFailure -> return current.recoveryCause()
            is HttpDataSource.InvalidContentTypeException,
            is HttpDataSource.CleartextNotPermittedException,
            -> return PlaybackRecoveryCause.Fatal
        }
        if (current.isTransportFailure()) transport = true
        current = current.cause?.takeUnless { it === current }
        depth++
    }
    return if (transport) PlaybackRecoveryCause.Network else PlaybackRecoveryCause.Fatal
}

@OptIn(UnstableApi::class)
private fun Throwable.isTransportFailure(): Boolean =
    this is HttpDataSource.HttpDataSourceException ||
        this is SocketException ||
        this is UnknownHostException ||
        this is InterruptedIOException ||
        this is SSLException

private fun SabrPlaybackRecoveryException.recoveryCause(): PlaybackRecoveryCause = when (action) {
    "retry_fresh_session" -> PlaybackRecoveryCause.Session
    "retry_fresh_session_lower_video_itag" -> PlaybackRecoveryCause.Rebuild
    else -> PlaybackRecoveryCause.Fatal
}

private fun CodedFailure.recoveryCause(): PlaybackRecoveryCause = when {
    failureCode in SESSION_FAILURE_CODES -> PlaybackRecoveryCause.Session
    failureCode != null && statusCode in GONE_STATUS_CODES -> PlaybackRecoveryCause.Fatal
    else -> statusCode?.recoveryCause() ?: PlaybackRecoveryCause.Fatal
}

@OptIn(UnstableApi::class)
private fun HttpDataSource.InvalidResponseCodeException.recoveryCause(): PlaybackRecoveryCause =
    responseCode.recoveryCause(sabrMedia = dataSpec.uri.pathSegments.isSabrPlaybackPayloadPath())

private fun Int.recoveryCause(sabrMedia: Boolean = true): PlaybackRecoveryCause = when {
    this == 408 || this == 429 || this in 500..599 -> PlaybackRecoveryCause.Server
    !sabrMedia -> PlaybackRecoveryCause.Fatal
    this == 202 -> PlaybackRecoveryCause.Server
    this == 404 || this == 409 || this == 410 -> PlaybackRecoveryCause.Session
    else -> PlaybackRecoveryCause.Fatal
}

private val SESSION_FAILURE_CODES = setOf(
    SABR_PREPARATION_TIMEOUT,
    SABR_CONTRACT_FAILURE_CODE,
    "sabr_playback_not_found",
)
private val GONE_STATUS_CODES = setOf(404, 409, 410)
private const val MAX_CAUSE_DEPTH = 8
