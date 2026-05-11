package dev.typetype.android.feature.player

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.session.MediaController
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamAudioSource
import dev.typetype.android.domain.stream.StreamSubtitleSource
import dev.typetype.android.domain.stream.StreamVideoSource
import dev.typetype.android.services.MergedStreamMediaKeys

internal fun bindStreamToController(
    controller: MediaController,
    stream: Stream,
    videoUrl: String,
    startMillis: Long,
    selectedQuality: String = "auto",
    selectedAudioKey: String? = null,
    selectedSubtitleKey: String? = null,
    defaultAudioLanguage: String = "",
    preferOriginalLanguage: Boolean = false,
) {
    val source = pickPlayableSource(
        stream = stream,
        selectedQuality = selectedQuality,
        selectedAudioKey = selectedAudioKey,
        defaultAudioLanguage = defaultAudioLanguage,
        preferOriginalLanguage = preferOriginalLanguage,
    ) ?: return
    val subtitle = stream.subtitles.firstOrNull { it.key == selectedSubtitleKey }
    applyTrackSelectionDefaults(
        controller = controller,
        selectedQuality = selectedQuality,
        defaultAudioLanguage = defaultAudioLanguage,
        preferOriginalLanguage = preferOriginalLanguage,
        subtitlesEnabled = subtitle != null,
    )
    val metadata = MediaMetadata.Builder()
        .setTitle(stream.title)
        .setArtist(stream.uploaderName)
        .setArtworkUri(Uri.parse(stream.thumbnailUrl))
        .build()
    val mediaItem = MediaItem.Builder()
        .setUri(source.url)
        .setMediaId(videoUrl)
        .setMediaMetadata(metadata)
        .setRequestMetadata(source.toRequestMetadata(subtitle))
        .apply { source.mimeType?.let { setMimeType(it) } }
        .apply { subtitle?.let { setSubtitleConfigurations(listOf(it.toSubtitleConfiguration())) } }
        .build()
    val currentItem = controller.currentMediaItem
    val sameMedia = currentItem?.mediaId == videoUrl
    val sameSource = sameMedia &&
        currentItem.localConfiguration?.uri?.toString() == source.url &&
        currentItem.requestMetadata.extras
            ?.getString(MergedStreamMediaKeys.EXTRA_AUDIO_URL) == source.audioUrl &&
        currentItem.requestMetadata.extras
            ?.getString(MergedStreamMediaKeys.EXTRA_SUBTITLE_URL) == subtitle?.url
    if (!sameSource) {
        val startPosition = if (sameMedia) controller.currentPosition else startMillis
        if (startPosition > 0) {
            controller.setMediaItem(mediaItem, startPosition)
        } else {
            controller.setMediaItem(mediaItem)
        }
        controller.prepare()
    }
    controller.playWhenReady = true
}

private data class PlayableSource(
    val url: String,
    val mimeType: String?,
    val audioUrl: String? = null,
    val audioMimeType: String? = null,
)

private fun pickPlayableSource(
    stream: Stream,
    selectedQuality: String,
    selectedAudioKey: String?,
    defaultAudioLanguage: String,
    preferOriginalLanguage: Boolean,
): PlayableSource? {
    val mergedSource = pickMergedSource(
        stream = stream,
        selectedQuality = selectedQuality,
        selectedAudioKey = selectedAudioKey,
        defaultAudioLanguage = defaultAudioLanguage,
        preferOriginalLanguage = preferOriginalLanguage,
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
    return pickMuxedSource(stream, selectedQuality)
        ?: stream.progressiveUrl?.let { PlayableSource(it, MimeTypes.VIDEO_MP4) }
}

private fun pickMergedSource(
    stream: Stream,
    selectedQuality: String,
    selectedAudioKey: String?,
    defaultAudioLanguage: String,
    preferOriginalLanguage: Boolean,
): PlayableSource? {
    if (selectedAudioKey == null) return null
    val video = stream.videoOnlyStreams.pickVideo(selectedQuality) ?: return null
    val audio = stream.audioStreams.pickAudio(
        selectedAudioKey = selectedAudioKey,
        defaultAudioLanguage = defaultAudioLanguage,
        preferOriginalLanguage = preferOriginalLanguage,
    ) ?: return null
    return PlayableSource(
        url = video.url,
        mimeType = video.mimeType.normalizedMimeType(),
        audioUrl = audio.url,
        audioMimeType = audio.mimeType.normalizedMimeType(),
    )
}

private fun pickMuxedSource(stream: Stream, defaultQuality: String): PlayableSource? =
    stream.muxedVideoStreams.pickVideo(defaultQuality)?.let { source ->
        PlayableSource(
            url = source.url,
            mimeType = source.mimeType.normalizedMimeType() ?: MimeTypes.VIDEO_MP4,
        )
    }

private fun List<StreamVideoSource>.pickVideo(defaultQuality: String): StreamVideoSource? {
    val playable = filter { it.url.isNotBlank() && it.height > 0 }
    val targetHeight = defaultQuality.qualityHeight()
    if (targetHeight == null) {
        return playable.maxWithOrNull(compareBy<StreamVideoSource> { it.height }.thenBy { it.bitrate ?: 0 })
    }
    return playable
        .filter { it.height <= targetHeight }
        .maxWithOrNull(compareBy<StreamVideoSource> { it.height }.thenBy { it.bitrate ?: 0 })
        ?: playable
            .minWithOrNull(compareBy<StreamVideoSource> { it.height }.thenByDescending { it.bitrate ?: 0 })
}

private fun List<StreamAudioSource>.pickAudio(
    selectedAudioKey: String?,
    defaultAudioLanguage: String,
    preferOriginalLanguage: Boolean,
): StreamAudioSource? {
    val playable = filter { it.url.isNotBlank() }
    val selected = selectedAudioKey?.let { key -> playable.firstOrNull { it.key == key } }
    if (selected != null) return selected
    val original = if (preferOriginalLanguage) playable.firstOrNull { it.isOriginal } else null
    if (original != null) return original
    val language = defaultAudioLanguage.takeIf { it.isNotBlank() }
    val localized = language?.let { target ->
        playable.firstOrNull { audio ->
            audio.audioLocale?.equals(target, ignoreCase = true) == true ||
                audio.audioLocale?.startsWith("$target-", ignoreCase = true) == true
        }
    }
    return localized ?: playable.maxByOrNull { it.bitrate ?: 0 }
}

private fun applyTrackSelectionDefaults(
    controller: MediaController,
    selectedQuality: String,
    defaultAudioLanguage: String,
    preferOriginalLanguage: Boolean,
    subtitlesEnabled: Boolean,
) {
    val targetHeight = selectedQuality.qualityHeight()
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

private fun PlayableSource.toRequestMetadata(subtitle: StreamSubtitleSource?): MediaItem.RequestMetadata {
    val audio = audioUrl?.takeIf { it.isNotBlank() }
    if (audio == null && subtitle == null) return MediaItem.RequestMetadata.EMPTY
    val extras = Bundle().apply {
        audio?.let { putString(MergedStreamMediaKeys.EXTRA_AUDIO_URL, it) }
        audioMimeType?.let { putString(MergedStreamMediaKeys.EXTRA_AUDIO_MIME_TYPE, it) }
        mimeType?.let { putString(MergedStreamMediaKeys.EXTRA_VIDEO_MIME_TYPE, it) }
        subtitle?.let { putString(MergedStreamMediaKeys.EXTRA_SUBTITLE_URL, it.url) }
    }
    return MediaItem.RequestMetadata.Builder()
        .setExtras(extras)
        .build()
}

private fun String.normalizedMimeType(): String? =
    substringBefore(";").trim().takeIf { it.isNotBlank() }

private fun String.qualityHeight(): Int? =
    filter { it.isDigit() }.toIntOrNull()

internal val StreamAudioSource.key: String
    get() = audioTrackId?.takeIf { it.isNotBlank() }
        ?: audioLocale?.takeIf { it.isNotBlank() }
        ?: url

internal val StreamSubtitleSource.key: String
    get() = languageTag.takeIf { it.isNotBlank() }
        ?.let { "$it:${isAutoGenerated}" }
        ?: url

private fun StreamSubtitleSource.toSubtitleConfiguration(): MediaItem.SubtitleConfiguration =
    MediaItem.SubtitleConfiguration.Builder(Uri.parse(url))
        .setMimeType(MimeTypes.TEXT_VTT)
        .setLanguage(languageTag.takeIf { it.isNotBlank() })
        .setLabel(displayLanguageName)
        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
        .build()
