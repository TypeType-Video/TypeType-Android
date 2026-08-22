package dev.typetype.android.feature.player

import dev.typetype.android.domain.stream.StreamVideoSource

internal const val AUTO_QUALITY_KEY = "auto"
internal const val RECOMMENDED_QUALITY_KEY = "recommended"
internal const val RECOMMENDED_CODEC_KEY = "recommended"
internal const val H264_CODEC_KEY = "h264"
internal const val HEVC_CODEC_KEY = "hevc"
internal const val VP9_CODEC_KEY = "vp9"
internal const val AV1_CODEC_KEY = "av1"
internal const val OTHER_CODEC_KEY = "other"
private const val VIDEO_ITAG_PREFIX = "itag:"
private const val VIDEO_FORMAT_PREFIX = "format:"

internal fun videoItagKey(itag: Int): String = "$VIDEO_ITAG_PREFIX$itag"

internal fun String.selectedVideoItag(): Int? =
    takeIf { it.startsWith(VIDEO_ITAG_PREFIX) }
        ?.removePrefix(VIDEO_ITAG_PREFIX)
        ?.toIntOrNull()

internal fun StreamVideoSource.videoSelectionKey(): String = if (itag > 0) {
    videoItagKey(itag)
} else {
    listOf(
        mimeType.substringBefore(';').trim().lowercase(),
        codec.orEmpty().lowercase(),
        width,
        height,
        fps,
        bitrate ?: 0,
        isVideoOnly,
    ).joinToString(prefix = VIDEO_FORMAT_PREFIX, separator = ":")
}

internal fun String.isExplicitVideoSelection(): Boolean =
    startsWith(VIDEO_ITAG_PREFIX) || startsWith(VIDEO_FORMAT_PREFIX)

internal fun String.selectedQualityHeight(): Int? =
    takeUnless { it.isExplicitVideoSelection() }
        ?.filter { it.isDigit() }
        ?.toIntOrNull()

internal fun String.effectiveQuality(automaticQualityCap: String): String =
    if (this == AUTO_QUALITY_KEY || this == RECOMMENDED_QUALITY_KEY) {
        automaticQualityCap
    } else {
        this
    }

internal fun StreamVideoSource.codecSelectionKey(): String {
    val normalized = codec.orEmpty().trim().lowercase()
    return when {
        normalized.startsWith("avc1") || normalized.startsWith("avc3") -> H264_CODEC_KEY
        normalized.startsWith("hvc1") || normalized.startsWith("hev1") -> HEVC_CODEC_KEY
        normalized.startsWith("vp9") || normalized.startsWith("vp09") -> VP9_CODEC_KEY
        normalized.startsWith("av01") -> AV1_CODEC_KEY
        else -> OTHER_CODEC_KEY
    }
}

internal fun String.codecDisplayName(): String = when (this) {
    H264_CODEC_KEY -> "H.264"
    HEVC_CODEC_KEY -> "HEVC"
    VP9_CODEC_KEY -> "VP9"
    AV1_CODEC_KEY -> "AV1"
    else -> "Other"
}
