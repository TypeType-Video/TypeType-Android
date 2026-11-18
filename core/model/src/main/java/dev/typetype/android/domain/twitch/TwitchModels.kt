package dev.typetype.android.domain.twitch

import dev.typetype.android.domain.feed.Video

data class TwitchCategory(
    val id: String,
    val name: String,
    val slug: String,
    val boxArtUrl: String?,
    val viewers: Long?,
)

data class TwitchPage<T>(val items: List<T>, val cursor: String?)

data class TwitchChannel(
    val login: String,
    val displayName: String,
    val avatarUrl: String?,
    val description: String,
    val bannerUrl: String?,
    val followers: Long,
    val live: Video?,
) {
    val url: String get() = TwitchUrls.live(login)
}

data class TwitchPlayback(val video: Video, val playlistUrl: String, val audioPlaylistUrl: String?, val live: Boolean)

data class TwitchClipQuality(val height: Int, val frameRate: Int, val url: String)

data class TwitchClipPlayback(val video: Video, val qualities: List<TwitchClipQuality>)

data class TwitchPollChoice(val id: String, val title: String, val votes: Long)

data class TwitchPoll(val id: String, val title: String, val votes: Long, val choices: List<TwitchPollChoice>)

data class TwitchPinnedMessage(val id: String, val text: String, val author: String)

data class TwitchChatHighlights(val poll: TwitchPoll?, val pinned: TwitchPinnedMessage?)

data class TwitchVideoComment(
    val id: String,
    val offsetSeconds: Long,
    val author: String?,
    val text: String,
    val moderator: Boolean,
)
