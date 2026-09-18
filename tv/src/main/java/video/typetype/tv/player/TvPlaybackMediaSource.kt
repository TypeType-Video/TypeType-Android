@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package video.typetype.tv.player

import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import video.typetype.sdk.core.SubtitleTrack
import video.typetype.sdk.core.TypeTypeClient
import video.typetype.sdk.core.TypeTypeError
import video.typetype.sdk.media3.PlaybackMediaSourceHandle
import video.typetype.sdk.media3.ManifestPlaybackRequest
import video.typetype.sdk.media3.createManifestMediaSource
import video.typetype.sdk.media3.createYoutubeSabrMediaSource
import video.typetype.sdk.media3.createAudioOnlyMediaSource
import video.typetype.sdk.core.AudioOnlyStream

internal data class TvPlaybackMediaSource(
    val handle: PlaybackMediaSourceHandle,
    val mediaSource: MediaSource,
    val subtitleError: TypeTypeError? = null,
)

internal suspend fun createTvPlaybackMediaSource(
    client: TypeTypeClient,
    request: TvPlaybackRequest,
    subtitle: SubtitleTrack?,
): TvPlaybackMediaSource {
    val handle: PlaybackMediaSourceHandle = when {
        request.isManifest -> createManifestMediaSource(
            ManifestPlaybackRequest(
                manifestUrl = requireNotNull(request.manifestUrl),
                protocol = requireNotNull(request.manifestProtocol),
                requestHeaders = client.sessions.current()?.accessToken?.value
                    ?.let { mapOf("Authorization" to "Bearer $it") }
                    .orEmpty(),
            ),
        )
        request.isAudioOnly -> {
            val session = client.sessions.current()
            createAudioOnlyMediaSource(
                AudioOnlyStream(
                    sourceUrl = requireNotNull(request.audioOnlyUrl),
                    mimeType = requireNotNull(request.audioOnlyMimeType),
                    codec = null,
                    bitrate = null,
                    contentLength = null,
                    durationSeconds = request.durationMilliseconds?.div(1_000L),
                    kind = request.audioOnlyKind,
                ),
                session?.accessToken?.value?.let { mapOf("Authorization" to "Bearer $it") }.orEmpty(),
            )
        }
        else -> createYoutubeSabrMediaSource(client, request.asSabrRequest())
    }
    try {
        val subtitleResult = subtitle?.let {
            withTimeoutOrNull(3_000L) {
                withContext(Dispatchers.IO) {
                    createTvSubtitleSource(client, request.videoId, it, request.durationMilliseconds)
                }
            }
                ?: TvSubtitleSourceResult(
                    mediaSource = null,
                    error = TypeTypeError.Network("Subtitle request timed out", null),
                )
        } ?: TvSubtitleSourceResult(mediaSource = null, error = null)
        val source = subtitleResult.mediaSource?.let { MergingMediaSource(true, handle.mediaSource, it) }
            ?: handle.mediaSource
        return TvPlaybackMediaSource(handle, source, subtitleResult.error)
    } catch (cancelled: CancellationException) {
        handle.close()
        throw cancelled
    } catch (failure: Exception) {
        handle.close()
        throw failure
    }
}
