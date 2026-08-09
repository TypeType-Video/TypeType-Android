package dev.typetype.android.feature.player

import dev.typetype.android.domain.stream.StreamAudioSource
import dev.typetype.android.domain.stream.StreamVideoSource

internal fun List<StreamVideoSource>.pickVideo(
    quality: String,
    codecSupport: PlaybackCodecSupport,
    selectedCodec: String = RECOMMENDED_CODEC_KEY,
): StreamVideoSource? {
    val playable = mapNotNull { source ->
        if (source.url.isBlank() || source.height <= 0) return@mapNotNull null
        val decoder = codecSupport.video(source)
        if (decoder == DecoderSupport.Unsupported) null else VideoCandidate(source, decoder)
    }
    quality.selectedVideoItag()?.let { selectedItag ->
        return playable.firstOrNull { it.source.itag == selectedItag }?.source
    }
    if (quality.isExplicitVideoSelection()) {
        return playable.firstOrNull { it.source.videoSelectionKey() == quality }?.source
    }
    val codecCandidates = playable.filter {
        selectedCodec == RECOMMENDED_CODEC_KEY ||
            it.source.codecSelectionKey() == selectedCodec
    }
    if (codecCandidates.isEmpty()) return null
    val targetHeight = quality.selectedQualityHeight()
    val comparator = if (selectedCodec == RECOMMENDED_CODEC_KEY) {
        recommendedVideoComparator
    } else {
        qualityFirstComparator
    }
    if (targetHeight == null) return codecCandidates.maxWithOrNull(comparator)?.source
    return codecCandidates
        .filter { it.source.height <= targetHeight }
        .maxWithOrNull(comparator)
        ?.source
        ?: codecCandidates.minWithOrNull(
            compareBy<VideoCandidate> { it.source.height }
                .thenByDescending { it.decoder.rank }
                .thenByDescending { it.source.codecReliability() },
        )?.source
}

internal fun List<StreamVideoSource>.playableLowerVideoItags(
    selected: StreamVideoSource,
    codecSupport: PlaybackCodecSupport,
    selectedCodec: String = RECOMMENDED_CODEC_KEY,
): Set<Int> {
    val recoveryCodec = selectedCodec.takeUnless { it == RECOMMENDED_CODEC_KEY }
        ?: selected.codecSelectionKey()
    return asSequence()
        .filter { it.itag > 0 && it.url.isNotBlank() && it.height > 0 }
        .filter { codecSupport.video(it) != DecoderSupport.Unsupported }
        .filter { it.codecSelectionKey() == recoveryCodec }
        .filter { videoQualityComparator.compare(it, selected) < 0 }
        .sortedWith(videoQualityComparator.reversed())
        .map { it.itag }
        .toCollection(linkedSetOf())
}

internal fun List<StreamAudioSource>.pickAudio(
    selectedAudioKey: String?,
    defaultAudioLanguage: String,
    preferOriginalLanguage: Boolean,
    codecSupport: PlaybackCodecSupport,
    preferredDefaultAudioTrackId: String? = null,
    originalAudioTrackId: String? = null,
): StreamAudioSource? {
    val playable = filter {
        it.url.isNotBlank() && codecSupport.audio(it) != DecoderSupport.Unsupported
    }
    val selected = selectedAudioKey?.let { key -> playable.firstOrNull { it.key == key } }
    if (selected != null) return selected
    val preferredDefault = playable.findTrack(preferredDefaultAudioTrackId)
    val original = playable.findTrack(originalAudioTrackId)
        ?: playable.firstOrNull { it.isOriginal }
    val localized = playable.findLanguage(defaultAudioLanguage)
    return if (preferOriginalLanguage) {
        original ?: preferredDefault ?: localized ?: playable.firstOrNull()
    } else {
        localized ?: preferredDefault ?: original ?: playable.firstOrNull()
    }
}

private fun List<StreamAudioSource>.findTrack(trackId: String?): StreamAudioSource? =
    trackId?.takeIf { it.isNotBlank() }?.let { id ->
        firstOrNull { it.audioTrackId == id }
    }

private fun List<StreamAudioSource>.findLanguage(language: String): StreamAudioSource? {
    val target = language.normalizedLanguageTag()
    if (target.isBlank()) return null
    return firstOrNull { source ->
        val candidate = source.audioLocale.orEmpty().normalizedLanguageTag()
        candidate == target || candidate.substringBefore('-') == target.substringBefore('-')
    }
}

private fun String.normalizedLanguageTag(): String = trim().lowercase().replace('_', '-')

private data class VideoCandidate(
    val source: StreamVideoSource,
    val decoder: DecoderSupport,
)

private val recommendedVideoComparator = compareBy<VideoCandidate> { it.decoder.rank }
    .thenBy { it.source.codecReliability() }
    .thenBy { it.source.height }
    .thenBy { it.source.fps }
    .thenBy { it.source.bitrate ?: 0 }

private val qualityFirstComparator = compareBy<VideoCandidate> { it.source.height }
    .thenBy { it.decoder.rank }
    .thenBy { it.source.codecReliability() }
    .thenBy { it.source.fps }
    .thenBy { it.source.bitrate ?: 0 }

private val videoQualityComparator = compareBy<StreamVideoSource> { it.height }
    .thenBy { it.width }
    .thenBy { it.bitrate ?: 0 }

private fun StreamVideoSource.codecReliability(): Int {
    val normalized = codec.orEmpty().lowercase()
    return when {
        normalized.startsWith("avc1") || normalized.startsWith("avc3") -> 4
        normalized.startsWith("hvc1") || normalized.startsWith("hev1") -> 3
        normalized.startsWith("vp9") || normalized.startsWith("vp09") -> 2
        normalized.startsWith("av01") -> 1
        mimeType.substringBefore(';').trim().equals("video/mp4", ignoreCase = true) -> 4
        else -> 0
    }
}
