package dev.typetype.android.data.diagnostics

import dev.typetype.android.data.network.CurrentServerEndpoint
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object NetworkRouteClassifier {
    private val topLevelRoutes = setOf(
        "auth",
        "blocked",
        "bug-reports",
        "channel",
        "comments",
        "downloader",
        "favorites",
        "health",
        "history",
        "instance",
        "playlists",
        "profile",
        "progress",
        "recommendations",
        "search",
        "search-history",
        "sabr",
        "sessions",
        "settings",
        "streams",
        "subscriptions",
        "suggestions",
        "trending",
        "watch-later",
    )

    private val safeSecondSegments = mapOf(
        "auth" to setOf("guest", "login", "logout", "me", "oidc", "refresh", "register"),
        "blocked" to setOf("channels", "videos"),
        "comments" to setOf("replies"),
        "downloader" to setOf("jobs"),
        "profile" to setOf("avatar"),
        "recommendations" to setOf("home"),
        "streams" to setOf("youtube"),
        "subscriptions" to setOf("feed"),
        "sessions" to setOf("activity", "playback"),
    )

    private val safeThirdSegments = mapOf(
        ("auth" to "oidc") to setOf("start", "callback"),
        ("streams" to "youtube") to setOf("sabr"),
        ("sessions" to "playback") to setOf("start", "progress", "stop"),
    )

    fun classify(endpoint: CurrentServerEndpoint, url: HttpUrl): String? {
        if (!endpoint.owns(url)) return null
        val baseUrl = endpoint.baseUrl.toHttpUrlOrNull() ?: return null
        val baseSegments = baseUrl.pathSegments.filter(String::isNotBlank)
        val requestSegments = url.pathSegments.filter(String::isNotBlank)
        if (requestSegments.take(baseSegments.size) != baseSegments) return null
        val relative = requestSegments.drop(baseSegments.size)
        if (relative.take(2) == listOf("sabr", "playback")) {
            return classifySabrPlayback(relative)
        }
        if (relative.take(4) == listOf("streams", "youtube", "sabr", "bootstrap")) {
            return "/streams/youtube/sabr/bootstrap"
        }
        val first = relative.firstOrNull()?.takeIf(topLevelRoutes::contains) ?: return "/other"
        val second = relative.getOrNull(1)?.takeIf { it in safeSecondSegments[first].orEmpty() }
        val third = relative.getOrNull(2)?.takeIf {
            it in safeThirdSegments[first to second].orEmpty()
        }
        return listOfNotNull(first, second, third).joinToString(separator = "/", prefix = "/")
    }

    private fun classifySabrPlayback(segments: List<String>): String {
        val operation = when {
            segments.size == 3 -> "create"
            segments.getOrNull(3) in sabrControlActions -> segments[3]
            segments.getOrNull(4) == "init" -> "init"
            segments.getOrNull(4) == "segment" -> "segment"
            else -> null
        }
        return listOfNotNull("sabr", "playback", operation)
            .joinToString(separator = "/", prefix = "/")
    }

    private val sabrControlActions = setOf(
        "seek",
        "position",
        "prefetch",
        "segments",
        "window",
        "manifest",
        "state",
    )
}
