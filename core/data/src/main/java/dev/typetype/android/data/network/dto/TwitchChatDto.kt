package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class TwitchChatEmoteDto(val id: String = "", val start: Int = 0, val end: Int = 0)

@Serializable
data class TwitchRoomStateDto(
    val followersOnly: Int = -1,
    val slow: Int = 0,
    val emoteOnly: Boolean = false,
    val subsOnly: Boolean = false,
    val uniqueChat: Boolean = false,
)

@Serializable
data class TwitchChatEventDto(
    val type: String = "",
    val id: String? = null,
    val login: String? = null,
    val displayName: String? = null,
    val color: String? = null,
    val text: String? = null,
    val system: String? = null,
    val emotes: List<TwitchChatEmoteDto> = emptyList(),
    val badges: List<String> = emptyList(),
    val time: String = "",
    val action: Boolean = false,
    val targetId: String? = null,
    val room: TwitchRoomStateDto? = null,
    val deleted: Boolean = false,
)

@Serializable
data class TwitchThirdPartyEmoteDto(
    val name: String = "",
    val url: String = "",
    val url2x: String? = null,
    val provider: String = "",
    val animated: Boolean = false,
    val zeroWidth: Boolean = false,
)

@Serializable
data class TwitchChatBadgeDto(val title: String = "", val url: String = "", val url2x: String? = null)

@Serializable
data class TwitchChatAssetsDto(
    val channelId: String? = null,
    val badges: Map<String, TwitchChatBadgeDto> = emptyMap(),
    val emotes: List<TwitchThirdPartyEmoteDto> = emptyList(),
)

@Serializable
data class TwitchPollChoiceDto(val id: String = "", val title: String = "", val votes: Long = 0)

@Serializable
data class TwitchPollDto(
    val id: String = "",
    val title: String = "",
    val status: String = "",
    val startedAt: String = "",
    val endedAt: String? = null,
    val durationSeconds: Long = 0,
    val remainingMs: Long = 0,
    val voters: Long = 0,
    val votes: Long = 0,
    val choices: List<TwitchPollChoiceDto> = emptyList(),
)

@Serializable
data class TwitchPinnedMessageDto(
    val id: String = "",
    val text: String = "",
    val login: String = "",
    val displayName: String = "",
    val pinnedBy: String? = null,
    val endsAt: String? = null,
)

@Serializable
data class TwitchChatHighlightsDto(val poll: TwitchPollDto? = null, val pinned: TwitchPinnedMessageDto? = null)

@Serializable
data class TwitchVideoCommentDto(
    val id: String = "",
    val offset: Long = 0,
    val login: String? = null,
    val displayName: String? = null,
    val color: String? = null,
    val text: String = "",
    val emotes: List<TwitchChatEmoteDto> = emptyList(),
    val badges: List<String> = emptyList(),
)
