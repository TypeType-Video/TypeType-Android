package dev.typetype.android.domain.twitch

import dev.typetype.android.domain.livechat.ChatEmote
import dev.typetype.android.domain.livechat.ChatEmoteRange
import dev.typetype.android.domain.livechat.ChatImage

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
    val liveInfo: TwitchLiveInfo? = null,
) {
    val url: String get() = TwitchUrls.live(login)
}

data class TwitchPlayback(
    val video: Video,
    val playlistUrl: String,
    val audioPlaylistUrl: String?,
    val live: Boolean,
    val startOverUrl: String? = null,
)

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
    val login: String? = null,
    val color: String? = null,
    val emotes: List<ChatEmoteRange> = emptyList(),
    val badges: List<String> = emptyList(),
)

data class TwitchLiveInfo(
    val title: String,
    val viewers: Long,
    val startedAtMillis: Long,
    val category: String?,
    val tags: List<String>,
)

// The emotes of BTTV, FFZ and 7TV keyed by the word that shows them, and the chat badges keyed as Twitch sends them
// on a message ("moderator/1"); the channel's own come before the global ones.
data class TwitchChatAssets(
    val emotes: Map<String, ChatEmote> = emptyMap(),
    val badges: Map<String, ChatImage> = emptyMap(),
)
