package dev.typetype.android.domain.stream

internal fun isServerSabrVideoFormat(codec: String?): Boolean {
    val normalized = codec?.trim()?.lowercase().orEmpty()
    return normalized.startsWith("avc1") ||
        normalized.startsWith("vp9") ||
        normalized.startsWith("vp09") ||
        normalized.startsWith("av01")
}

internal fun isServerSabrAudioFormat(mimeType: String, codec: String?): Boolean =
    mimeType.substringBefore(';').trim().equals("audio/mp4", ignoreCase = true) &&
        codec?.trim()?.startsWith("mp4a", ignoreCase = true) == true
