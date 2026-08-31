package video.typetype.tv.data

import video.typetype.sdk.core.PlaybackSession
import video.typetype.sdk.core.ServiceId
import video.typetype.sdk.core.StreamDetails

internal data class StandardManifest(
    val url: String,
    val protocol: String,
)

internal fun StreamDetails.standardManifest(): StandardManifest? {
    val hls = hlsUrl?.takeIf(String::isNotBlank)
    val dash = dashMpdUrl?.takeIf(String::isNotBlank)
    return when {
        isLive && hls != null -> StandardManifest(hls, "hls")
        dash != null -> StandardManifest(dash, "dash")
        hls != null -> StandardManifest(hls, "hls")
        else -> null
    }
}

internal fun StreamDetails.standardPlaybackSession(service: ServiceId): PlaybackSession? {
    if (service == ServiceId.YOUTUBE) return null
    val manifest = standardManifest() ?: return null
    return PlaybackSession(
        sessionId = "manifest-${id.value}",
        videoId = id,
        formats = emptyList(),
        audioTracks = emptyList(),
        subtitles = emptyList(),
        isLive = isLive,
        ready = true,
        status = "ready",
        manifestUrl = manifest.url,
        startTimeMilliseconds = startPositionMilliseconds,
        durationMilliseconds = durationSeconds * 1_000L,
        transport = "manifest",
        protocol = manifest.protocol,
    )
}
