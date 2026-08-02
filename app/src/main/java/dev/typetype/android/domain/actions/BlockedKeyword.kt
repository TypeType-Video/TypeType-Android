package dev.typetype.android.domain.actions

import java.text.Normalizer
import java.util.Locale

data class BlockedKeyword(
    val keyword: String,
    val blockedAt: Long,
    val global: Boolean,
)

fun titleMatchesBlockedKeyword(title: String, keywords: Collection<String>): Boolean {
    val normalizedTitle = normalizeBlockedKeyword(title)
    return keywords.any { keyword ->
        normalizeBlockedKeyword(keyword).takeIf(String::isNotEmpty)?.let(normalizedTitle::contains) == true
    }
}

private fun normalizeBlockedKeyword(value: String): String = Normalizer
    .normalize(value, Normalizer.Form.NFKC)
    .trim()
    .lowercase(Locale.ROOT)
