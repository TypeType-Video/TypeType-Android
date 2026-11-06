package dev.typetype.android.core.url

import java.net.URI

fun buildImageUrl(serverBaseUrl: String?, imageUrl: String): String {
    val source = imageUrl.trim().let { value ->
        when {
            value.startsWith("httpss://") -> "https://${value.removePrefix("httpss://")}"
            value.startsWith("//") -> "https:$value"
            else -> value
        }
    }.let(::sizedBilibiliImage)
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

// Bilibili covers and avatars are several hundred kilobytes at full size; its image service scales
// them on request, to the sizes the web client asks for.
private fun sizedBilibiliImage(source: String): String {
    val uri = runCatching { URI(source) }.getOrNull() ?: return source
    val host = uri.host?.lowercase() ?: return source
    if (!host.endsWith("hdslb.com") && !host.endsWith("biliimg.com")) return source
    val path = uri.rawPath ?: return source
    val size = when {
        '@' in path -> return source
        path.startsWith("/bfs/archive/") -> BILIBILI_COVER_SIZE
        path.startsWith("/bfs/face/") -> BILIBILI_AVATAR_SIZE
        else -> return source
    }
    val sizedHost = if (host.endsWith("biliimg.com")) "i0.hdslb.com" else host
    return "https://$sizedHost$path$size" + uri.rawQuery?.let { "?$it" }.orEmpty()
}

private const val BILIBILI_COVER_SIZE = "@672w_378h_1c.webp"
private const val BILIBILI_AVATAR_SIZE = "@240w_240h_1c.webp"
