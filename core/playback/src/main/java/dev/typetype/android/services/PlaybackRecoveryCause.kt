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

// What a playback failure asks for. Every recovery layer reads this instead of matching exceptions itself.
internal enum class PlaybackRecoveryCause {
    // No route or a broken connection: retry once a network is there.
    Network,

    // The server answered with a temporary error or is still busy: retry the same request later.
    Server,

    // The server lost, refused or stalled the session: a fresh session at the same position plays on.
    Session,

    // The server switched quality or needs a lower one: the media item has to be rebuilt.
    Rebuild,

    // Nothing to retry; the user sees why.
    Fatal,
}

// The deepest failure that says something wins: Media3 and the window coordinator wrap the real cause in a plain
// IOException, which alone does not mean the network failed.
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
    failureCode == SABR_PREPARATION_TIMEOUT || failureCode == SABR_CONTRACT_FAILURE_CODE ->
        PlaybackRecoveryCause.Session
    else -> statusCode?.recoveryCause() ?: PlaybackRecoveryCause.Fatal
}

// A missing subtitle or thumbnail is not a lost session: only SABR media requests say anything about the session.
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

private const val MAX_CAUSE_DEPTH = 8
