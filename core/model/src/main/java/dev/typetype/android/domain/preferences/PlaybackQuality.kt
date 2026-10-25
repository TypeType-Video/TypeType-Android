package dev.typetype.android.domain.preferences

private val QUALITY_HEIGHT = Regex("""(\d{3,4})p""", RegexOption.IGNORE_CASE)

val PLAYBACK_QUALITY_OPTIONS = listOf(
    "auto",
    "144p",
    "240p",
    "360p",
    "480p",
    "720p",
    "1080p",
    "1440p",
    "2160p",
)

val PLAYBACK_MOBILE_QUALITY_OPTIONS = listOf(
    "480p",
    "720p",
    "1080p",
    "1440p",
    "2160p",
)

fun normalizePlaybackQuality(value: String): String {
    val cleaned = value.trim().lowercase()
    if (cleaned == "auto" || cleaned == "best") return "auto"
    val height = QUALITY_HEIGHT.find(cleaned)?.groupValues?.get(1)?.toIntOrNull()
        ?: return "1080p"
    return "${height}p"
}

fun effectivePlaybackQualityCap(
    defaultQuality: String,
    mobileDataQuality: String,
    isMetered: Boolean,
): String {
    val normalizedDefault = normalizePlaybackQuality(defaultQuality)
    if (!isMetered) return normalizedDefault.capHeightLabel()
    val normalizedMobile = normalizePlaybackQuality(mobileDataQuality)
    return "${minOf(normalizedDefault.capHeight(), normalizedMobile.capHeight())}p"
}

private fun String.capHeight(): Int =
    if (this == "auto") 2160 else substringBefore('p').toIntOrNull() ?: 1080

private fun String.capHeightLabel(): String =
    if (this == "auto") "2160p" else this
