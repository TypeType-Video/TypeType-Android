package dev.typetype.android.domain.usersettings

private val QUALITY_HEIGHT = Regex("""\d+""")

fun normalizeQualityName(value: String): String {
    val cleaned = value.trim()
    val match = QUALITY_HEIGHT.find(cleaned) ?: return cleaned.ifBlank { "1080p" }
    val height = "${match.value}p"
    return if (QUALITY_HDR.containsMatchIn(cleaned)) "$height HDR" else height
}

private val QUALITY_HDR = Regex("""\bHDR\b""", RegexOption.IGNORE_CASE)
