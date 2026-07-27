package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PodcastItemDto(
    val id: String,
    val title: String,
    val url: String,
    val thumbnailUrl: String = "",
    val uploaderName: String = "",
    val streamCount: Long = 0L,
    val playlistType: String = "",
)

@Serializable
data class ChannelPodcastPageDto(
    val channelName: String = "",
    val channelUrl: String = "",
    val podcasts: List<PodcastItemDto> = emptyList(),
    val episodes: List<VideoItem> = emptyList(),
    val nextpage: String? = null,
)

@Serializable
data class PodcastEpisodesPageDto(
    val podcast: PodcastItemDto,
    val episodes: List<VideoItem> = emptyList(),
    val nextpage: String? = null,
)
