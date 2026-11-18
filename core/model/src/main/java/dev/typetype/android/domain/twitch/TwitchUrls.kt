package dev.typetype.android.domain.twitch

import java.net.URI

sealed interface TwitchTarget {
    data class Live(val login: String) : TwitchTarget
    data class Vod(val id: String) : TwitchTarget
    data class Clip(val slug: String) : TwitchTarget
}

object TwitchUrls {
    fun live(login: String) = "https://www.twitch.tv/${login.lowercase()}"

    fun vod(id: String) = "https://www.twitch.tv/videos/$id"

    fun clip(slug: String) = "https://clips.twitch.tv/$slug"

    fun parse(url: String): TwitchTarget? {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return null
        val host = uri.host?.lowercase() ?: return null
        val segments = uri.path.orEmpty().split('/').filter { it.isNotEmpty() }
        if (host == "clips.twitch.tv") return segments.singleOrNull()?.let(TwitchTarget::Clip)
        if (host != "twitch.tv" && !host.endsWith(".twitch.tv")) return null
        return when {
            segments.size == 2 && segments[0] == "videos" && segments[1].all(Char::isDigit) -> TwitchTarget.Vod(segments[1])
            segments.size == 3 && segments[1] == "clip" -> TwitchTarget.Clip(segments[2])
            segments.size == 1 && LOGIN.matches(segments[0]) && segments[0].lowercase() !in RESERVED ->
                TwitchTarget.Live(segments[0].lowercase())
            else -> null
        }
    }

    private val LOGIN = Regex("[A-Za-z0-9_]{1,25}")
    private val RESERVED = setOf(
        "directory", "search", "settings", "videos", "downloads", "drops", "inventory", "jobs", "p", "prime",
        "store", "subscriptions", "turbo", "wallet",
    )
}
