package dev.typetype.android.feature.player

import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackSelection
import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamPlaybackContract
import dev.typetype.android.domain.stream.accept
import dev.typetype.android.domain.stream.binding
import dev.typetype.android.domain.stream.isServerSabrAudioFormat
import dev.typetype.android.domain.stream.isServerSabrVideoFormat
import dev.typetype.android.domain.stream.sabrPlaybackTarget
import dev.typetype.android.domain.stream.sourceKey
import dev.typetype.android.services.MergedStreamMediaKeys
import dev.typetype.android.services.sabrPlaybackBinding
import dev.typetype.android.services.sabrPlaybackMediaUri
import dev.typetype.android.services.sabrPlaybackTarget

internal typealias PrepareSabrPlayback =
    suspend (Stream, SabrPlaybackSelection, Long) -> SabrPlaybackSession?

internal suspend fun pickSabrSource(
    stream: Stream,
    selectedQuality: String,
    selectedAudioKey: String?,
    defaultAudioLanguage: String,
    preferOriginalLanguage: Boolean,
    codecSupport: PlaybackCodecSupport,
    startTimeMs: Long = 0L,
    prepareSabrPlayback: PrepareSabrPlayback,
    selectedCodec: String = RECOMMENDED_CODEC_KEY,
): PlayableSource? {
    val requestedSelection = stream.sabrSelection(
        selectedQuality = selectedQuality,
        selectedAudioKey = selectedAudioKey,
        defaultAudioLanguage = defaultAudioLanguage,
        preferOriginalLanguage = preferOriginalLanguage,
        codecSupport = codecSupport,
        selectedCodec = selectedCodec,
    ) ?: return null
    val requestedTarget = stream.sabrPlaybackTarget(requestedSelection)
    val session = prepareSabrPlayback(stream, requestedSelection, startTimeMs) ?: return null
    val acceptedTarget = requestedTarget.accept(session)
    return PlayableSource(
        url = sabrPlaybackMediaUri(session.sessionId),
        mimeType = TYPE_TYPE_SABR_MIME_TYPE,
        sourceKey = acceptedTarget.sourceKey,
        sabrRequestKey = stream.sabrRequestKey(requestedSelection),
        sabrBinding = session.binding,
        sabrTarget = acceptedTarget,
        sabrSession = session,
    )
}

private fun Stream.sabrSelection(
    selectedQuality: String,
    selectedAudioKey: String?,
    defaultAudioLanguage: String,
    preferOriginalLanguage: Boolean,
    codecSupport: PlaybackCodecSupport,
    selectedCodec: String,
): SabrPlaybackSelection? {
    if (playbackContract != StreamPlaybackContract.ServerSabr || requestScope == null) return null
    val videos = sabrVideoStreams.filter {
        it.itag > 0 && it.url.isNotBlank() && isServerSabrVideoFormat(it.codec)
    }
    val video = videos
        .pickVideo(selectedQuality, codecSupport, selectedCodec) ?: return null
    val audio = sabrAudioStreams.filter {
        it.itag > 0 && it.itag != video.itag && it.url.isNotBlank() &&
            isServerSabrAudioFormat(it.mimeType, it.codec)
    }.pickAudio(
        selectedAudioKey = selectedAudioKey,
        defaultAudioLanguage = defaultAudioLanguage,
        preferOriginalLanguage = preferOriginalLanguage,
        codecSupport = codecSupport,
    ) ?: return null
    return SabrPlaybackSelection(
        video = video,
        audio = audio,
        recoveryVideoItags = videos.playableLowerVideoItags(video, codecSupport, selectedCodec),
    )
}

internal fun Stream.sabrRequestKey(
    selectedQuality: String,
    selectedAudioKey: String?,
    defaultAudioLanguage: String,
    preferOriginalLanguage: Boolean,
    codecSupport: PlaybackCodecSupport,
    selectedCodec: String = RECOMMENDED_CODEC_KEY,
): String? = sabrSelection(
    selectedQuality,
    selectedAudioKey,
    defaultAudioLanguage,
    preferOriginalLanguage,
    codecSupport,
    selectedCodec,
)?.let(::sabrRequestKey)

internal fun MediaItem.reusableSabrSource(requestKey: String?): PlayableSource? {
    val extras = requestMetadata.extras
    return reusableSabrSource(
        requestKey = requestKey,
        storedRequestKey = extras?.getString(MergedStreamMediaKeys.EXTRA_SABR_REQUEST_KEY),
        acceptedKey = extras?.getString(MergedStreamMediaKeys.EXTRA_SOURCE_KEY),
        url = localConfiguration?.uri?.toString(),
        sabrBinding = extras?.sabrPlaybackBinding(),
        sabrTarget = extras?.sabrPlaybackTarget(),
    )
}

internal fun reusableSabrSource(
    requestKey: String?,
    storedRequestKey: String?,
    acceptedKey: String?,
    url: String?,
    sabrBinding: SabrPlaybackBinding?,
    sabrTarget: SabrPlaybackTarget?,
): PlayableSource? {
    val expected = requestKey ?: return null
    if (storedRequestKey != expected) return null
    val sourceUrl = url?.takeIf { it.isNotBlank() } ?: return null
    val sourceKey = acceptedKey?.takeIf { it.isNotBlank() } ?: return null
    val binding = sabrBinding ?: return null
    val target = sabrTarget ?: return null
    if (
        target.videoItag != binding.videoItag || target.audioItag != binding.audioItag ||
        target.audioTrackId != binding.audioTrackId || target.sourceKey != sourceKey
    ) return null
    return PlayableSource(
        url = sourceUrl,
        mimeType = TYPE_TYPE_SABR_MIME_TYPE,
        sourceKey = sourceKey,
        sabrRequestKey = expected,
        sabrBinding = binding,
        sabrTarget = target,
    )
}

private const val TYPE_TYPE_SABR_MIME_TYPE = "application/x-typetype-sabr"

private fun Stream.sabrRequestKey(selection: SabrPlaybackSelection): String {
    val scope = requireNotNull(requestScope)
    return listOf(
        "sabr",
        scope.serverId,
        scope.accountId,
        scope.baseUrl.trimEnd('/'),
        id,
        selection.video.itag,
        selection.audio.itag,
        selection.audio.audioTrackId.orEmpty(),
        isLive,
    ).joinToString("\u0000")
}
