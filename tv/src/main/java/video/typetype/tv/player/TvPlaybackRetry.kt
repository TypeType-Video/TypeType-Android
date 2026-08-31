package video.typetype.tv.player

import android.os.Bundle
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import video.typetype.sdk.core.SubtitleTrack

internal fun schedulePlaybackRetry(
    scope: CoroutineScope,
    player: ExoPlayer,
    session: MediaSession,
    request: TvPlaybackRequest,
    subtitle: SubtitleTrack?,
    positionMilliseconds: Long,
    replace: suspend (TvPlaybackRequest, SubtitleTrack?, Long, Boolean) -> Unit,
    startSponsorBlock: (TvPlaybackRequest) -> Unit,
    onStarted: () -> Unit,
): Job = scope.launch {
    try {
        player.stop()
        replace(request, subtitle, positionMilliseconds, true)
        onStarted()
        startSponsorBlock(request)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        session.setSessionExtras(Bundle().apply {
            putString(PLAYBACK_ERROR_EXTRA, failure.message ?: "Playback retry failed")
        })
    }
}
