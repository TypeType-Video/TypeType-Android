package dev.typetype.android.feature.player

import androidx.media3.common.MimeTypes
import dev.typetype.android.domain.stream.Stream

internal fun Stream.pickExplicitProviderSource(
    selectedQuality: String,
    selectedAudioKey: String?,
    defaultAudioLanguage: String,
    preferOriginalLanguage: Boolean,
    codecSupport: PlaybackCodecSupport,
    selectedCodec: String = RECOMMENDED_CODEC_KEY,
): PlayableSource? {
    if (!selectedQuality.isExplicitVideoSelection()) return null
    val video = (videoOnlyStreams + muxedVideoStreams)
        .pickVideo(selectedQuality, codecSupport, selectedCodec) ?: return null
    if (!video.isVideoOnly) {
        return PlayableSource(
            url = video.url,
            mimeType = video.mimeType.normalizedProviderMimeType() ?: MimeTypes.VIDEO_MP4,
            sourceKey = video.videoSelectionKey(),
        )
    }
    val audio = audioStreams.pickAudio(
        selectedAudioKey = selectedAudioKey,
        defaultAudioLanguage = defaultAudioLanguage,
        preferOriginalLanguage = preferOriginalLanguage,
        codecSupport = codecSupport,
        preferredDefaultAudioTrackId = preferredDefaultAudioTrackId,
        originalAudioTrackId = originalAudioTrackId,
    ) ?: return null
    return PlayableSource(
        url = video.url,
        mimeType = video.mimeType.normalizedProviderMimeType(),
        audioUrl = audio.url,
        audioMimeType = audio.mimeType.normalizedProviderMimeType(),
        sourceKey = video.videoSelectionKey(),
    )
}

private fun String.normalizedProviderMimeType(): String? =
    substringBefore(';').trim().takeIf { it.isNotBlank() }
