package dev.typetype.android.domain.navigation

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

fun resolveIncomingVideoUrl(value: String?): String? =
    resolveVideoSource(value?.trim().orEmpty(), depth = 0)

fun resolveSharedVideoUrl(value: String?): String? {
    val text = value?.trim().orEmpty()
    if (text.isEmpty() || text.length > MAX_SHARED_TEXT_LENGTH) return null
    resolveIncomingVideoUrl(text)?.let { return it }
    return URL_PATTERN.findAll(text)
        .map { match -> match.value.trimEnd(*TRAILING_PUNCTUATION) }
        .mapNotNull(::resolveIncomingVideoUrl)
        .firstOrNull()
}

private fun resolveVideoSource(value: String, depth: Int): String? {
    if (value.isEmpty() || value.length > MAX_URL_LENGTH || depth > MAX_WRAPPER_DEPTH) return null
    if (YOUTUBE_VIDEO_ID_PATTERN.matches(value)) {
        return "https://www.youtube.com/watch?v=$value"
    }
    if (NICONICO_VIDEO_ID_PATTERN.matches(value)) {
        return "https://www.nicovideo.jp/watch/$value"
    }
    BILIBILI_WATCH_PARAM_PATTERN.matchEntire(value)?.let { match ->
        val page = match.groupValues[2].toIntOrNull()?.takeIf { it > 1 }
        return buildString {
            append("https://www.bilibili.com/video/")
            append(match.groupValues[1])
            page?.let { append("?p=$it") }
        }
    }

    val uri = runCatching { URI(value) }.getOrNull() ?: return null
    return when (uri.scheme?.lowercase()) {
        "typetype" -> resolveWatchWrapper(uri, depth)
        "http", "https" -> resolveWebUrl(uri, depth)
        else -> null
    }
}

private fun resolveWebUrl(uri: URI, depth: Int): String? {
    if (uri.host.isNullOrBlank() || uri.userInfo != null) return null
    youtubeVideoId(uri)?.let { return "https://www.youtube.com/watch?v=$it" }
    niconicoVideoId(uri)?.let { return "https://www.nicovideo.jp/watch/$it" }
    bilibiliWatchParam(uri)?.let { return resolveVideoSource(it, depth + 1) }
    if (uri.path.orEmpty().trimEnd('/') == "/watch") {
        return queryParameter(uri, "v")?.let { value -> resolveVideoSource(value, depth + 1) }
    }
    return uri.toString().takeIf { isSupportedVideoHost(uri.host.lowercase()) }
}

private fun isSupportedVideoHost(host: String): Boolean =
    host == "youtu.be" ||
        hostMatches(host, "youtube.com") ||
        host == "nico.ms" ||
        hostMatches(host, "nicovideo.jp") ||
        host == "b23.tv" ||
        hostMatches(host, "bilibili.com")

private fun youtubeVideoId(uri: URI): String? {
    val host = uri.host.lowercase()
    val pathParts = uri.path.orEmpty().split('/').filter(String::isNotEmpty)
    val watchId = queryParameter(uri, "v")
    val candidate = when {
        host == "youtu.be" -> pathParts.firstOrNull()
        !hostMatches(host, "youtube.com") -> null
        watchId != null -> watchId
        pathParts.firstOrNull() in YOUTUBE_VIDEO_PATHS -> pathParts.getOrNull(1)
        else -> null
    }
    return candidate?.takeIf(YOUTUBE_VIDEO_ID_PATTERN::matches)
}

private fun niconicoVideoId(uri: URI): String? {
    val host = uri.host.lowercase()
    val pathParts = uri.path.orEmpty().split('/').filter(String::isNotEmpty)
    val candidate = when {
        host == "nico.ms" -> pathParts.firstOrNull()
        hostMatches(host, "nicovideo.jp") && pathParts.firstOrNull() == "watch" -> {
            pathParts.getOrNull(1)
        }
        else -> null
    }
    return candidate
        ?.takeIf(NICONICO_VIDEO_ID_PATTERN::matches)
}

private fun bilibiliWatchParam(uri: URI): String? {
    if (!hostMatches(uri.host.lowercase(), "bilibili.com")) return null
    val pathParts = uri.path.orEmpty().split('/').filter(String::isNotEmpty)
    val id = pathParts.getOrNull(1)
        ?.takeIf { pathParts.firstOrNull() == "video" }
        ?.takeIf(BILIBILI_VIDEO_ID_PATTERN::matches)
        ?: return null
    val page = queryParameter(uri, "p")?.toIntOrNull()?.takeIf { it > 1 }
    return if (page == null) id else "$id?p=$page"
}

private fun hostMatches(host: String, domain: String): Boolean =
    host == domain || host.endsWith(".$domain")

private fun resolveWatchWrapper(uri: URI, depth: Int): String? {
    val target = when {
        uri.host.equals("watch", ignoreCase = true) -> queryParameter(uri, "v")
        uri.path.orEmpty().trimEnd('/') == "/watch" -> queryParameter(uri, "v")
        else -> null
    }
    return target?.let { resolveVideoSource(it, depth + 1) }
}

private fun queryParameter(uri: URI, name: String): String? = uri.rawQuery
    ?.split('&')
    ?.asSequence()
    ?.map { parameter -> parameter.substringBefore('=') to parameter.substringAfter('=', "") }
    ?.firstOrNull { (key) -> decode(key) == name }
    ?.second
    ?.let(::decode)
    ?.trim()
    ?.takeIf(String::isNotEmpty)

private fun decode(value: String): String = runCatching {
    URLDecoder.decode(value, StandardCharsets.UTF_8.name())
}.getOrDefault(value)

private val YOUTUBE_VIDEO_ID_PATTERN = Regex("^[A-Za-z0-9_-]{11}$")
private val NICONICO_VIDEO_ID_PATTERN = Regex("^sm[0-9]+$", RegexOption.IGNORE_CASE)
private val BILIBILI_WATCH_PARAM_PATTERN =
    Regex("^(BV[A-Za-z0-9]{10})(?:[?]p=([0-9]+))?$", RegexOption.IGNORE_CASE)
private val BILIBILI_VIDEO_ID_PATTERN = Regex("^BV[A-Za-z0-9]{10}$", RegexOption.IGNORE_CASE)
private val YOUTUBE_VIDEO_PATHS = setOf("embed", "live", "shorts")
private val URL_PATTERN = Regex("(?:https?://|typetype://)[^\\s<>]+", RegexOption.IGNORE_CASE)
private val TRAILING_PUNCTUATION = charArrayOf('.', ',', ';', ':', '!', '?', ')', ']', '}', '"', '\'')
private const val MAX_URL_LENGTH = 8_192
private const val MAX_SHARED_TEXT_LENGTH = 32_768
private const val MAX_WRAPPER_DEPTH = 2
