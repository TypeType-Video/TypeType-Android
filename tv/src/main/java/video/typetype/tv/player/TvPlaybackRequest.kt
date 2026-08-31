package video.typetype.tv.player

import video.typetype.sdk.media3.ManifestProtocol
import video.typetype.sdk.media3.SabrPlaybackRequest

internal data class TvVideoTrack(
    val itag: Int,
    val mimeType: String,
)

internal data class TvPlaybackRequest(
    val sessionId: String,
    val videoId: String,
    val videoUrl: String,
    val title: String,
    val thumbnailUrl: String,
    val durationMilliseconds: Long?,
    val videoItag: Int?,
    val audioItag: Int?,
    val audioTrackId: String?,
    val generation: Long,
    val startTimeMilliseconds: Long,
    val videoMimeType: String?,
    val audioMimeType: String?,
    val videoTracks: List<TvVideoTrack>,
    val manifestUrl: String?,
    val manifestProtocol: ManifestProtocol?,
    val audioOnlyUrl: String?,
    val audioOnlyMimeType: String?,
    val audioOnlyKind: String?,
    val trackProgress: Boolean,
    val playbackSpeed: Float,
    val playbackVolume: Float,
    val sponsorBlockPolicy: SponsorBlockPolicy,
) {
    val isManifest: Boolean get() = manifestUrl != null
    val isAudioOnly: Boolean get() = audioOnlyUrl != null

    fun asSabrRequest(): SabrPlaybackRequest = SabrPlaybackRequest(
        sessionId = sessionId,
        videoId = videoId,
        videoUrl = videoUrl,
        title = title,
        thumbnailUrl = thumbnailUrl,
        durationMilliseconds = durationMilliseconds,
        videoItag = requireNotNull(videoItag),
        audioItag = requireNotNull(audioItag),
        audioTrackId = audioTrackId,
        generation = generation,
        startTimeMilliseconds = startTimeMilliseconds,
        videoMimeType = requireNotNull(videoMimeType),
        audioMimeType = requireNotNull(audioMimeType),
    )
}
