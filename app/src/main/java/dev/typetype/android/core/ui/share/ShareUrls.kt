package dev.typetype.android.core.ui.share

import androidx.compose.runtime.compositionLocalOf
import dev.typetype.android.domain.navigation.toPublicWatchParameter
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

val LocalServerBaseUrl = compositionLocalOf<String?> { null }

fun buildShareUrl(serverBaseUrl: String?, videoUrl: String): String {
    if (serverBaseUrl.isNullOrBlank()) return videoUrl
    val origin = serverBaseUrl
        .trimEnd('/')
        .removeSuffix("/api")
        .trimEnd('/')
    if (origin.isBlank()) return videoUrl
    val encoded = URLEncoder.encode(toPublicWatchParameter(videoUrl), StandardCharsets.UTF_8.toString())
    return "$origin/watch?v=$encoded"
}

fun buildImageUrl(serverBaseUrl: String?, imageUrl: String): String {
    val source = imageUrl.trim().let { value ->
        if (value.startsWith("httpss://")) "https://${value.removePrefix("httpss://")}" else value
    }
    val base = serverBaseUrl?.trim()?.trimEnd('/')
    if (source.isBlank() || base.isNullOrBlank()) return source

    val origin = base.removeSuffix("/api")
    if (source.startsWith('/')) return "$origin$source"
    if (source.startsWith("$origin/")) return source
    if (!source.startsWith("http://") && !source.startsWith("https://")) return source
    if (!needsImageProxy(source)) return source

    val encoded = URLEncoder.encode(source, StandardCharsets.UTF_8.toString())
    return "$base/proxy?url=$encoded"
}

private fun needsImageProxy(source: String): Boolean {
    val host = runCatching { URI(source).host?.lowercase() }.getOrNull() ?: return false
    return host.endsWith("ggpht.com") ||
        host.endsWith("googleusercontent.com") ||
        host.endsWith("hdslb.com") ||
        host.endsWith("ytimg.com")
}
