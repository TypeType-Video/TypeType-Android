package dev.typetype.android.data.channel

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal fun buildChannelRequestUrl(
    channelUrl: String,
    searchQuery: String,
    live: Boolean,
): String {
    val query = searchQuery.trim()
    if (!live && query.isEmpty()) return channelUrl
    val source = splitChannelSearchUrl(channelUrl).first.toHttpUrlOrNull() ?: return channelUrl
    if (!source.host.isYouTubeHost()) return channelUrl
    val suffix = if (live) "streams" else "search"
    return source.newBuilder()
        .encodedPath("${source.encodedPath.trimEnd('/')}/$suffix")
        .query(null)
        .fragment(null)
        .apply { if (!live) addQueryParameter("query", query) }
        .build()
        .toString()
}

internal fun splitChannelSearchUrl(url: String): Pair<String, String> {
    val fallback = url.trim().trimEnd('/')
    val parsed = fallback.toHttpUrlOrNull() ?: return fallback to ""
    val segments = parsed.pathSegments.filter(String::isNotBlank)
    val searchIndex = segments.indexOfLast { it == "search" }
    if (!parsed.host.isYouTubeHost() || searchIndex <= 0) return fallback to ""
    val channelPath = segments.take(searchIndex).joinToString("/", prefix = "/")
    val channelUrl = parsed.newBuilder()
        .encodedPath(channelPath)
        .query(null)
        .fragment(null)
        .build()
        .toString()
        .trimEnd('/')
    return channelUrl to (parsed.queryParameter("query")?.trim().orEmpty())
}

private fun String.isYouTubeHost(): Boolean = this == "youtube.com" || endsWith(".youtube.com")
