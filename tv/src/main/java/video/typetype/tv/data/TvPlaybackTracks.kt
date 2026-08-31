package video.typetype.tv.data

import video.typetype.sdk.core.SabrTrackSelection
import video.typetype.sdk.core.StreamDetails
import video.typetype.sdk.core.StreamVideo
import video.typetype.sdk.core.UserSettings
import video.typetype.sdk.core.isSabrPlaybackTrack
import video.typetype.sdk.core.selectSabrPlaybackTracks

internal fun StreamDetails.selectTvPlaybackTracks(
    isVideoSupported: (StreamVideo) -> Boolean,
    preferredVideoItag: Int? = null,
    preferredAudioItag: Int? = null,
    preferredAudioTrackId: String? = null,
    preferredQuality: String = "auto",
): SabrTrackSelection? {
    val allSupportedVideos = videoOnlyStreams
        .filter(StreamVideo::isSabrPlaybackTrack)
        .filter(isVideoSupported)
    val maximumHeight = preferredQuality.removeSuffix("p").toIntOrNull()
    val preferredVideos = maximumHeight?.let { height ->
        allSupportedVideos.filter { it.height in 1..height }.takeIf(List<StreamVideo>::isNotEmpty)
    } ?: allSupportedVideos
    val supportedVideos = preferredVideos
    if (supportedVideos.isEmpty()) return null
    return copy(videoOnlyStreams = supportedVideos).selectSabrPlaybackTracks(
        preferredVideoItag = preferredVideoItag,
        preferredAudioItag = preferredAudioItag,
        preferredAudioTrackId = preferredAudioTrackId,
    )
}

internal fun StreamDetails.defaultTvAudioTrackId(settings: UserSettings): String? {
    if (settings.preferOriginalLanguage) return originalAudioTrackId ?: preferredDefaultAudioTrackId
    val preferredLanguage = settings.defaultAudioLanguage.trim().lowercase().takeIf(String::isNotEmpty)
    return preferredLanguage?.let { language ->
        audioStreams.firstOrNull { audio ->
            val locale = audio.audioLocale?.lowercase().orEmpty()
            locale == language || locale.substringBefore('-') == language.substringBefore('-')
        }?.audioTrackId
    } ?: preferredDefaultAudioTrackId ?: originalAudioTrackId
}
