package dev.typetype.android.feature.player

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.session.MediaController
import androidx.core.net.toUri
import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamAudioSource
import dev.typetype.android.domain.stream.StreamPlaybackContract
import dev.typetype.android.domain.stream.StreamSubtitleSource
import dev.typetype.android.services.MergedStreamMediaKeys
import dev.typetype.android.services.sabrPlaybackBinding
import dev.typetype.android.services.sabrPlaybackTarget
import dev.typetype.android.services.sabrMediaTimeMs
import dev.typetype.android.services.toSubtitleConfigurations

internal suspend fun bindStreamToController(
    controller: MediaController,
    stream: Stream,
    videoUrl: String,
    startMillis: Long,
    selectedQuality: String = "auto",
    selectedAudioKey: String? = null,
    selectedSubtitleKey: String? = null,
    defaultAudioLanguage: String = "",
    automaticQualityCap: String = "",
    preferOriginalLanguage: Boolean = false,
    initialPlayWhenReady: Boolean = true,
    codecSupport: PlaybackCodecSupport,
    prepareSabrPlayback: PrepareSabrPlayback,
    selectedCodec: String = RECOMMENDED_CODEC_KEY,
) {
    val currentItem = controller.currentMediaItem
    val sameMedia = currentItem?.mediaId == videoUrl
    val sourceStartMillis = if (sameMedia) {
        controller.sabrMediaTimeMs(controller.currentPosition, stream.isLive) ?: startMillis
    } else {
        startMillis
    }
    val sabrRequestKey = stream.sabrRequestKey(
        selectedQuality = selectedQuality.effectiveQuality(automaticQualityCap),
        selectedAudioKey = selectedAudioKey,
        defaultAudioLanguage = defaultAudioLanguage,
        preferOriginalLanguage = preferOriginalLanguage,
        codecSupport = codecSupport,
        selectedCodec = selectedCodec,
    )
    val reusableSabrSource = currentItem
        ?.takeIf { sameMedia && controller.playerError == null }
        ?.reusableSabrSource(sabrRequestKey)
    val source = reusableSabrSource ?: pickPlayableSource(
        stream = stream,
        selectedQuality = selectedQuality,
        selectedAudioKey = selectedAudioKey,
        defaultAudioLanguage = defaultAudioLanguage,
        automaticQualityCap = automaticQualityCap,
        preferOriginalLanguage = preferOriginalLanguage,
        codecSupport = codecSupport,
        startTimeMs = sourceStartMillis,
        prepareSabrPlayback = prepareSabrPlayback,
        selectedCodec = selectedCodec,
    ) ?: return
    applyTrackSelectionDefaults(
        controller = controller,
        selectedQuality = selectedQuality,
        defaultAudioLanguage = defaultAudioLanguage,
        automaticQualityCap = automaticQualityCap,
        preferOriginalLanguage = preferOriginalLanguage,
        subtitlesEnabled = selectedSubtitleKey != null,
    )
    val itemStartPosition = if (sameMedia) {
        controller.currentPosition.coerceAtLeast(0L)
    } else {
        startMillis.coerceAtLeast(0L)
    }
    val mediaItem = buildResolvedMediaItem(
        stream = stream,
        videoUrl = videoUrl,
        source = source,
        subtitles = if (stream.playbackContract == StreamPlaybackContract.ServerSabr) {
            emptyList()
        } else {
            source.playbackSubtitles(stream.subtitles)
        },
        startPositionMillis = itemStartPosition,
    )
    val extras = currentItem?.requestMetadata?.extras
    val sameSource = sameMedia && samePlayableSource(
        currentUrl = currentItem.localConfiguration?.uri?.toString(),
        currentSourceKey = extras?.getString(MergedStreamMediaKeys.EXTRA_SOURCE_KEY),
        currentAudioUrl = extras?.getString(MergedStreamMediaKeys.EXTRA_AUDIO_URL),
        currentSabrBinding = extras?.sabrPlaybackBinding(),
        currentSabrTarget = extras?.sabrPlaybackTarget(),
        requestedSource = source,
    )
    val requestedPlayWhenReady = replacementPlayWhenReady(
        stream.playbackContract,
        sameMedia,
        controller.playWhenReady,
        initialPlayWhenReady,
    )
    if (!sameSource) {
        if (itemStartPosition > 0) {
            controller.setMediaItem(mediaItem, itemStartPosition)
        } else {
            controller.setMediaItem(mediaItem)
        }
        controller.prepare()
    } else if (controller.playerError != null) {
        controller.prepare()
    }
    controller.playWhenReady = requestedPlayWhenReady
}

internal fun buildResolvedMediaItem(
    stream: Stream,
    videoUrl: String,
    source: PlayableSource,
    subtitles: List<StreamSubtitleSource>,
    startPositionMillis: Long,
): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(stream.title)
        .setArtist(stream.uploaderName)
        .setArtworkUri(stream.thumbnailUrl.toUri())
        .build()
    return MediaItem.Builder()
        .setUri(source.url)
        .setMediaId(videoUrl)
        .setMediaMetadata(metadata)
        .setRequestMetadata(
            source.toRequestMetadata(
                scope = stream.requestScope,
                resumePositionMillis = startPositionMillis.coerceAtLeast(0L),
                isLiveContent = stream.isLiveContent || stream.isLive,
                stream = stream,
            ),
        )
        .apply { source.mimeType?.let { setMimeType(it) } }
        .setSubtitleConfigurations(subtitles.toSubtitleConfigurations())
        .build()
}

internal fun replacementPlayWhenReady(
    playbackContract: StreamPlaybackContract,
    sameMedia: Boolean,
    currentPlayWhenReady: Boolean,
    initialPlayWhenReady: Boolean = true,
): Boolean = if (sameMedia && playbackContract == StreamPlaybackContract.ServerSabr) {
    currentPlayWhenReady
} else {
    initialPlayWhenReady
}

internal fun samePlayableSource(
    currentUrl: String?,
    currentSourceKey: String?,
    currentAudioUrl: String?,
    currentSabrBinding: SabrPlaybackBinding? = null,
    currentSabrTarget: SabrPlaybackTarget? = null,
    requestedSource: PlayableSource,
): Boolean = currentUrl == requestedSource.url &&
    currentSourceKey == requestedSource.sourceKey &&
    currentAudioUrl == requestedSource.audioUrl &&
    currentSabrBinding == requestedSource.sabrBinding &&
    currentSabrTarget == requestedSource.sabrTarget

internal suspend fun pickPlayableSource(
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
    if (!stream.serverHlsManifestUrl.isNullOrBlank()) {
        return PlayableSource(stream.serverHlsManifestUrl, MimeTypes.APPLICATION_M3U8)
    }
    if (!stream.serverDashManifestUrl.isNullOrBlank()) {
        return PlayableSource(stream.serverDashManifestUrl, MimeTypes.APPLICATION_MPD)
    }
    if (!stream.dashMpdUrl.isNullOrBlank()) {
        return PlayableSource(stream.dashMpdUrl, MimeTypes.APPLICATION_MPD)
    }
    if (!stream.hlsUrl.isNullOrBlank()) {
        return PlayableSource(stream.hlsUrl, MimeTypes.APPLICATION_M3U8)
    }
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
    if (selectedAudioKey == null) return null
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
    ) ?: return null
    return PlayableSource(
        url = video.url,
        mimeType = video.mimeType.normalizedMimeType(),
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
            mimeType = source.mimeType.normalizedMimeType() ?: MimeTypes.VIDEO_MP4,
        )
    }

private fun applyTrackSelectionDefaults(
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

private fun String.effectiveQuality(automaticQualityCap: String): String =
    if (this == AUTO_QUALITY_KEY || this == RECOMMENDED_QUALITY_KEY) {
        automaticQualityCap
    } else {
        this
    }

internal val StreamAudioSource.key: String
    get() = audioTrackId?.takeIf { it.isNotBlank() }
        ?: audioLocale?.takeIf { it.isNotBlank() }
        ?: url

internal val StreamSubtitleSource.key: String
    get() = trackId?.takeIf { it.isNotBlank() }
        ?: languageTag.takeIf { it.isNotBlank() }
        ?.let { "$it:${isAutoGenerated}" }
        ?: url

internal fun PlayableSource.playbackSubtitles(
    fallback: List<StreamSubtitleSource>,
): List<StreamSubtitleSource> = fallback
