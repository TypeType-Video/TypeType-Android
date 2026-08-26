package dev.typetype.android.domain.usersettings

private val QUALITY_HEIGHT = Regex("""\d+""")

fun normalizeQualityName(value: String): String {
    val cleaned = value.trim()
    val hasHdr = cleaned.contains("HDR", ignoreCase = true)
    val height = QUALITY_HEIGHT.find(cleaned)?.value.orEmpty()
    if (height.isEmpty()) return cleaned.ifBlank { "1080p" }
    return "${height}p${" HDR".takeIf { hasHdr } ?: ""}"
}
