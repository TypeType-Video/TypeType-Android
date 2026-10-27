package dev.typetype.android.feature.settings.blocked

import java.net.URI

internal fun blockedItemDisplayPath(value: String): String {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return "/"
    if (trimmed.startsWith('/')) return trimmed

    val uri = runCatching { URI(trimmed) }.getOrNull() ?: return "/$trimmed"
    val host = uri.host.orEmpty().lowercase()
    val path = uri.rawPath.orEmpty()
    val videoId = when {
        host == "youtu.be" || host.endsWith(".youtu.be") ->
            path.trim('/').substringBefore('/').takeIf(String::isNotBlank)
        host == "youtube.com" || host.endsWith(".youtube.com") ->
            uri.rawQuery.orEmpty().split('&')
                .firstOrNull { it.substringBefore('=') == "v" }
                ?.substringAfter('=', "")
                ?.takeIf(String::isNotBlank)
        else -> null
    }
    if (videoId != null) return "/$videoId"

    val visiblePath = path.ifBlank { "/" }
    return uri.rawQuery?.takeIf(String::isNotBlank)?.let { "$visiblePath?$it" } ?: visiblePath
}
