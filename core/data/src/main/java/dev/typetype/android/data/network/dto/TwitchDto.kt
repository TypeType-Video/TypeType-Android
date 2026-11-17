package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class TwitchPageDto<T>(
    val items: List<T> = emptyList(),
    val cursor: String? = null,
    val hasMore: Boolean = false,
)

@Serializable
data class TwitchGameDto(
    val id: String = "",
    val name: String = "",
    val slug: String = "",
    val boxArtUrl: String? = null,
    val viewers: Long? = null,
    val channels: Long? = null,
)

@Serializable
data class TwitchUserDto(
    val id: String = "",
    val login: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
)

@Serializable
data class TwitchArchiveDto(val id: String = "", val startedAt: String = "")

@Serializable
data class TwitchStreamDto(
    val id: String = "",
    val title: String = "",
    val user: TwitchUserDto = TwitchUserDto(),
    val game: TwitchGameDto? = null,
    val viewers: Long = 0,
    val previewUrl: String = "",
    val startedAt: String = "",
    val tags: List<String> = emptyList(),
    val archive: TwitchArchiveDto? = null,
)

@Serializable
data class TwitchChannelDto(
    val id: String = "",
    val login: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val description: String = "",
    val bannerUrl: String? = null,
    val followers: Long = 0,
    val partner: Boolean = false,
    val live: TwitchStreamDto? = null,
)

@Serializable
data class TwitchVideoDto(
    val id: String = "",
    val title: String = "",
    val owner: TwitchUserDto = TwitchUserDto(),
    val game: TwitchGameDto? = null,
    val lengthSeconds: Long = 0,
    val views: Long = 0,
    val previewUrl: String = "",
    val createdAt: String = "",
    val type: String = "",
    val recording: Boolean = false,
)

@Serializable
data class TwitchClipDto(
    val slug: String = "",
    val title: String = "",
    val broadcaster: TwitchUserDto = TwitchUserDto(),
    val game: TwitchGameDto? = null,
    val durationSeconds: Double = 0.0,
    val views: Long = 0,
    val thumbnailUrl: String = "",
    val createdAt: String = "",
)

@Serializable
data class TwitchClipQualityDto(val quality: String = "", val frameRate: Double = 0.0, val url: String = "")

@Serializable
data class TwitchLiveDto(val channel: TwitchChannelDto = TwitchChannelDto(), val hlsUrl: String = "", val audioUrl: String? = null)

@Serializable
data class TwitchVideoPlaybackDto(val video: TwitchVideoDto = TwitchVideoDto(), val hlsUrl: String = "", val audioUrl: String? = null)

@Serializable
data class TwitchClipPlaybackDto(val clip: TwitchClipDto = TwitchClipDto(), val qualities: List<TwitchClipQualityDto> = emptyList())

@Serializable
data class TwitchGameStreamsDto(val game: TwitchGameDto? = null, val streams: TwitchPageDto<TwitchStreamDto> = TwitchPageDto())

@Serializable
data class TwitchVideoChapterDto(val start: Long = 0, val title: String = "", val game: TwitchGameDto? = null)
