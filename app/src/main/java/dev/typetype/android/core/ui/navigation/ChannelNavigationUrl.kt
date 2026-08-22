package dev.typetype.android.core.ui.navigation

private val YOUTUBE_CHANNEL_ID = Regex("^UC[A-Za-z0-9_-]{22}$")
private val YOUTUBE_HANDLE = Regex("^@[A-Za-z0-9._-]{2,48}$")

internal fun channelNavigationUrl(rawValue: String): String {
    val value = rawValue.trim()
    return when {
        YOUTUBE_CHANNEL_ID.matches(value) -> "https://www.youtube.com/channel/$value"
        YOUTUBE_HANDLE.matches(value) -> "https://www.youtube.com/$value"
        value.startsWith("/channel/") || value.startsWith("/@") ->
            "https://www.youtube.com$value"
        value.startsWith("channel/") || value.startsWith("@") ->
            "https://www.youtube.com/$value"
        value.startsWith("youtube.com/", ignoreCase = true) ||
            value.startsWith("www.youtube.com/", ignoreCase = true) -> "https://$value"
        else -> value
    }
}
