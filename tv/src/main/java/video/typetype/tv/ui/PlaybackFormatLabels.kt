package video.typetype.tv.ui

import java.util.Locale

internal fun playbackCodecLabel(codec: String?): String? {
    val normalized = codec?.trim()?.lowercase(Locale.ROOT)?.takeIf(String::isNotEmpty) ?: return null
    return when {
        normalized.startsWith("av01") || normalized == "av1" -> "AV1"
        normalized.startsWith("vp09") || normalized.startsWith("vp9") -> "VP9"
        normalized.startsWith("avc1") || normalized.contains("h264") -> "H.264"
        normalized.startsWith("hev1") || normalized.startsWith("hvc1") ||
            normalized.contains("hevc") || normalized.contains("h265") -> "HEVC"
        normalized.startsWith("mp4a") || normalized.contains("aac") -> "AAC"
        normalized.contains("opus") -> "Opus"
        normalized.startsWith("ec-3") || normalized.contains("eac3") -> "E-AC-3"
        normalized.startsWith("ac-3") || normalized.contains("ac3") -> "AC-3"
        else -> normalized.substringBefore('.').uppercase(Locale.ROOT)
    }
}

internal fun playbackBitrateLabel(bitrate: Long): String {
    val kilobits = if (bitrate >= 1_000L) bitrate / 1_000L else bitrate
    return "$kilobits kbps"
}
