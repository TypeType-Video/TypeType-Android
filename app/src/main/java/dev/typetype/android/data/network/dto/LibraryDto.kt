package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class HistoryItemDto(
    val id: String,
    val url: String,
    val title: String,
    val thumbnail: String,
    val channelName: String,
    val channelUrl: String,
    val channelAvatar: String = "",
    val duration: Long,
    val progress: Long,
    val watchedAt: Long,
)

@Serializable
data class FavoriteItemDto(
    val videoUrl: String,
    val favoritedAt: Long,
)

@Serializable
data class WatchLaterItemDto(
    val url: String,
    val title: String,
    val thumbnail: String,
    val duration: Long,
    val addedAt: Long,
)

@Serializable
data class PlaylistVideoDto(
    val id: String,
    val url: String,
    val title: String,
    val thumbnail: String,
    val duration: Long,
    val position: Int,
    val channelName: String = "",
    val channelUrl: String = "",
    val channelAvatar: String = "",
    val viewCount: Long = 0L,
)

@Serializable
data class PlaylistDto(
    val id: String,
    val name: String,
    val description: String = "",
    val videos: List<PlaylistVideoDto> = emptyList(),
    val createdAt: Long,
)
