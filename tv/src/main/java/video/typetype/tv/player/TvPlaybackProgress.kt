package video.typetype.tv.player

import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import video.typetype.sdk.core.PlaybackProgress
import video.typetype.sdk.core.PlaybackWindowRequest
import video.typetype.sdk.core.TypeTypeClient

internal fun scheduleProgressUpdates(
    scope: CoroutineScope,
    client: TypeTypeClient,
    player: ExoPlayer,
    request: () -> TvPlaybackRequest?,
): Job = scope.launch {
    while (isActive) {
        delay(15_000L)
        val current = request() ?: continue
        if (!current.trackProgress) continue
        val storedSession = withContext(Dispatchers.IO) { client.sessions.current() } ?: continue
        if (storedSession.isGuest) continue
        val position = player.currentPosition.coerceAtLeast(0L)
        withContext(Dispatchers.IO) {
            client.library.updateProgress(
                PlaybackProgress(
                    videoUrl = current.videoUrl,
                    positionMilliseconds = position,
                    durationMilliseconds = current.durationMilliseconds,
                    watchedAtEpochSeconds = System.currentTimeMillis() / 1_000L,
                ),
            )
        }
    }
}

internal fun schedulePlaybackPositionUpdates(
    scope: CoroutineScope,
    client: TypeTypeClient,
    player: ExoPlayer,
    request: () -> TvPlaybackRequest?,
): Job = scope.launch {
    while (isActive) {
        delay(5_000L)
        val current = request() ?: continue
        if (current.isManifest || current.isAudioOnly) continue
        val positionRequest = PlaybackWindowRequest(
            generation = current.generation,
            playerTimeMilliseconds = player.currentPosition.coerceAtLeast(0L),
            videoItag = requireNotNull(current.videoItag),
            audioItag = requireNotNull(current.audioItag),
            audioTrackId = current.audioTrackId,
            playbackRate = player.playbackParameters.speed,
        )
        withContext(Dispatchers.IO) {
            client.playback.position(current.sessionId, positionRequest)
        }
    }
}
