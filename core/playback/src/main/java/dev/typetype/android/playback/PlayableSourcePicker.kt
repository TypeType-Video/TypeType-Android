package dev.typetype.android.playback
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.session.MediaController
import dev.typetype.android.domain.preferences.RECOMMENDED_CODEC_KEY
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamAudioSource
import dev.typetype.android.domain.stream.StreamPlaybackContract
import dev.typetype.android.domain.stream.StreamSubtitleSource
import dev.typetype.android.domain.stream.StreamVideoSource

suspend fun pickPlayableSource(
    stream: Stream,
    selectedQuality: String,
    selectedAudioKey: String?,
    defaultAudioLanguage: String,
    automaticQualityCap: String,
    preferOriginalLanguage: Boolean,
    codecSupport: PlaybackCodecSupport,
    startTimeMs: Long = 0L,
    prepareSabrPlayback: PrepareSabrPlayback,
    selectedCodec: String = RECOMMENDED_CODEC_KEY,
): PlayableSource? {
    val effectiveQuality = selectedQuality.effectiveQuality(automaticQualityCap)
    if (stream.playbackContract == StreamPlaybackContract.ServerSabr) {
        return pickSabrSource(
            stream = stream,
            selectedQuality = effectiveQuality,
            selectedAudioKey = selectedAudioKey,
            defaultAudioLanguage = defaultAudioLanguage,
            preferOriginalLanguage = preferOriginalLanguage,
            codecSupport = codecSupport,
            startTimeMs = startTimeMs,
            prepareSabrPlayback = prepareSabrPlayback,
            selectedCodec = selectedCodec,
        )
    }
    stream.pickExplicitProviderSource(
        selectedQuality = effectiveQuality,
        selectedAudioKey = selectedAudioKey,
        defaultAudioLanguage = defaultAudioLanguage,
        preferOriginalLanguage = preferOriginalLanguage,
        codecSupport = codecSupport,
        selectedCodec = selectedCodec,
    )?.let { return it }
    val mergedSource = pickMergedSource(
        stream = stream,
        selectedQuality = effectiveQuality,
        selectedAudioKey = selectedAudioKey,
        defaultAudioLanguage = defaultAudioLanguage,
        preferOriginalLanguage = preferOriginalLanguage,
        codecSupport = codecSupport,
        selectedCodec = selectedCodec,
    )
    if (mergedSource != null) return mergedSource
    stream.serverHlsManifestUrl?.takeIf(String::isNotBlank)?.let { return PlayableSource(it, MimeTypes.APPLICATION_M3U8) }
    stream.serverDashManifestUrl?.takeIf(String::isNotBlank)?.let { return PlayableSource(it, MimeTypes.APPLICATION_MPD) }
    stream.dashMpdUrl?.takeIf(String::isNotBlank)?.let { return PlayableSource(it, MimeTypes.APPLICATION_MPD) }
    stream.hlsUrl?.takeIf(String::isNotBlank)?.let { return PlayableSource(it, MimeTypes.APPLICATION_M3U8) }
    return pickMuxedSource(stream, effectiveQuality, codecSupport, selectedCodec)
        ?: stream.progressiveUrl?.let { PlayableSource(it, MimeTypes.VIDEO_MP4) }
}

private fun pickMergedSource(
    stream: Stream,
    selectedQuality: String,
    selectedAudioKey: String?,
    defaultAudioLanguage: String,
    preferOriginalLanguage: Boolean,
    codecSupport: PlaybackCodecSupport,
    selectedCodec: String,
): PlayableSource? {
    val video = stream.videoOnlyStreams.pickVideo(
        selectedQuality,
        codecSupport,
        selectedCodec,
    ) ?: return null
    val audio = stream.audioStreams.pickAudio(
        selectedAudioKey = selectedAudioKey,
        defaultAudioLanguage = defaultAudioLanguage,
        preferOriginalLanguage = preferOriginalLanguage,
        codecSupport = codecSupport,
        preferredDefaultAudioTrackId = stream.preferredDefaultAudioTrackId,
        originalAudioTrackId = stream.originalAudioTrackId,
    ) ?: return null
    return PlayableSource(
        url = video.url,
        mimeType = video.playbackContainerMimeType(),
        audioUrl = audio.url,
        audioMimeType = audio.mimeType.normalizedMimeType(),
    )
}

private fun pickMuxedSource(
    stream: Stream,
    defaultQuality: String,
    codecSupport: PlaybackCodecSupport,
    selectedCodec: String,
): PlayableSource? =
    stream.muxedVideoStreams.pickVideo(defaultQuality, codecSupport, selectedCodec)?.let { source ->
        PlayableSource(
            url = source.url,
            mimeType = source.playbackContainerMimeType() ?: MimeTypes.VIDEO_MP4,
        )
    }

fun applyTrackSelectionDefaults(
    controller: MediaController,
    selectedQuality: String,
    defaultAudioLanguage: String,
    automaticQualityCap: String,
    preferOriginalLanguage: Boolean,
    subtitlesEnabled: Boolean,
) {
    val targetHeight = selectedQuality.effectiveQuality(automaticQualityCap).selectedQualityHeight()
    val builder = controller.trackSelectionParameters.buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
        .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
    if (targetHeight != null) {
        builder.setMaxVideoSize(Int.MAX_VALUE, targetHeight)
    } else {
        builder.clearVideoSizeConstraints()
    }
    if (!preferOriginalLanguage && defaultAudioLanguage.isNotBlank()) {
        builder.setPreferredAudioLanguage(defaultAudioLanguage)
    }
    builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !subtitlesEnabled)
    controller.trackSelectionParameters = builder.build()
}

private fun String.normalizedMimeType(): String? =
    substringBefore(";").trim().takeIf { it.isNotBlank() }

internal fun StreamVideoSource.playbackContainerMimeType(): String? =
    (playbackMimeType ?: mimeType).normalizedMimeType()

val StreamAudioSource.key: String
    get() = audioTrackId?.takeIf { it.isNotBlank() }
        ?: audioLocale?.takeIf { it.isNotBlank() }
        ?: url

val StreamSubtitleSource.key: String
    get() = trackId?.takeIf { it.isNotBlank() }
        ?: languageTag.takeIf { it.isNotBlank() }
        ?.let { "$it:${isAutoGenerated}" }
        ?: url

fun PlayableSource.playbackSubtitles(
    fallback: List<StreamSubtitleSource>,
): List<StreamSubtitleSource> = fallback
