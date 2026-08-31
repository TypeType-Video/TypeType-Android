package video.typetype.tv.player

import android.os.Bundle
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import video.typetype.sdk.core.PlaybackOpenRequest
import video.typetype.sdk.core.TypeTypeClient
import video.typetype.sdk.core.TypeTypeResult
import video.typetype.sdk.media3.SabrPlaybackRestartRequiredException
import video.typetype.sdk.core.SubtitleTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal suspend fun reopenWithLowerVideoFormat(
    client: TypeTypeClient,
    request: TvPlaybackRequest,
    failure: SabrPlaybackRestartRequiredException,
    positionMilliseconds: Long,
): TvPlaybackRequest? {
    val track = failure.videoItags.asSequence()
        .mapNotNull { itag -> request.videoTracks.firstOrNull { it.itag == itag } }
        .firstOrNull { it.itag != request.videoItag } ?: return null
    val result = client.playback.open(
        PlaybackOpenRequest(
            videoUrl = request.videoUrl,
            videoItag = track.itag,
            audioItag = request.audioItag,
            audioTrackId = request.audioTrackId,
            startTimeMilliseconds = positionMilliseconds.coerceAtLeast(0L),
        ),
    )
    val session = (result as? TypeTypeResult.Success)?.value ?: return null
    if (
        !session.ready || session.videoId.value != request.videoId ||
        session.selectedVideoItag != track.itag || session.selectedAudioItag != request.audioItag ||
        session.audioTrackId != request.audioTrackId
    ) return null
    return request.copy(
        sessionId = session.sessionId,
        videoItag = track.itag,
        videoMimeType = track.mimeType,
        generation = session.generation,
        startTimeMilliseconds = session.startTimeMilliseconds.coerceAtLeast(positionMilliseconds),
        durationMilliseconds = session.durationMilliseconds ?: request.durationMilliseconds,
    )
}

internal fun Throwable.findSabrPlaybackRestartRequired(): SabrPlaybackRestartRequiredException? {
    var current: Throwable? = this
    repeat(16) {
        if (current is SabrPlaybackRestartRequiredException) return current
        current = current?.cause
    }
    return null
}

internal fun scheduleLowerFormatRecovery(
    scope: CoroutineScope,
    client: TypeTypeClient,
    player: ExoPlayer,
    session: MediaSession,
    request: TvPlaybackRequest,
    subtitle: SubtitleTrack?,
    failure: SabrPlaybackRestartRequiredException,
    positionMilliseconds: Long,
    replace: suspend (TvPlaybackRequest, SubtitleTrack?, Long, Boolean) -> Unit,
    stopSponsorBlock: () -> Unit,
    startSponsorBlock: (TvPlaybackRequest) -> Unit,
    onReplaced: (TvPlaybackRequest) -> Unit,
): Job = scope.launch {
    val next = try {
        withContext(Dispatchers.IO) {
            reopenWithLowerVideoFormat(client, request, failure, positionMilliseconds)
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        return@launch
    } ?: return@launch
    val resumePlayback = player.playWhenReady
    stopSponsorBlock()
    try {
        player.stop()
        replace(next, subtitle, positionMilliseconds, resumePlayback)
        onReplaced(next)
        startSponsorBlock(next)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        session.setSessionExtras(Bundle().apply {
            putString(PLAYBACK_ERROR_EXTRA, "SABR could not restart with a compatible video format")
        })
    }
}
