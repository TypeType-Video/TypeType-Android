package dev.typetype.android.feature.player

import androidx.media3.session.MediaController
import dev.typetype.android.domain.preferences.RECOMMENDED_CODEC_KEY
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamPlaybackContract
import dev.typetype.android.playback.PlaybackCodecSupport
import dev.typetype.android.playback.PrepareSabrPlayback
import dev.typetype.android.playback.applyTrackSelectionDefaults
import dev.typetype.android.playback.buildResolvedMediaItem
import dev.typetype.android.playback.effectiveQuality
import dev.typetype.android.playback.pickPlayableSource
import dev.typetype.android.playback.playbackSubtitles
import dev.typetype.android.playback.reusableSabrSource
import dev.typetype.android.playback.sabrRequestKey
import dev.typetype.android.playback.samePlayableSource
import dev.typetype.android.services.MergedStreamMediaKeys
import dev.typetype.android.services.sabrMediaTimeMs
import dev.typetype.android.services.sabrPlaybackBinding
import dev.typetype.android.services.sabrPlaybackTarget

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
    val sourceStartMillis = replacementSourceStartTimeMs(
        sameMedia = sameMedia,
        live = stream.isLive,
        reusingCurrentSource = reusableSabrSource != null,
        currentMediaTimeMs = controller.sabrMediaTimeMs(
            controller.currentPosition,
            stream.isLive,
        ),
        requestedStartTimeMs = startMillis,
    )
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
    val itemStartPosition = replacementPlayerPositionMs(
        sameMedia = sameMedia,
        live = stream.isLive,
        reusingCurrentSource = reusableSabrSource != null,
        currentPositionMs = controller.currentPosition,
        requestedPositionMs = startMillis,
    )
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
        preferOriginalAudio = preferOriginalLanguage,
        preferredAudioLocale = defaultAudioLanguage,
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
