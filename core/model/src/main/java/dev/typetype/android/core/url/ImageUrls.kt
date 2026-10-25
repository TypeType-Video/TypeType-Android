package dev.typetype.android.core.url

import java.net.URI

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

    val encoded = percentEncode(source)
    return "$base/proxy?url=$encoded"
}

private fun needsImageProxy(source: String): Boolean {
    val host = runCatching { URI(source).host?.lowercase() }.getOrNull() ?: return false
    return host.endsWith("ggpht.com") ||
        host.endsWith("googleusercontent.com") ||
        host.endsWith("hdslb.com") ||
        host.endsWith("ytimg.com")
}
