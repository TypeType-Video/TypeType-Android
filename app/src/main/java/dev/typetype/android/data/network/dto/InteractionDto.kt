package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class AddWatchLaterRequest(
    val url: String,
    val title: String,
    val thumbnail: String,
    val duration: Long,
    val channelName: String = "",
    val channelUrl: String = "",
    val channelAvatar: String = "",
    val viewCount: Long = 0L,
)

@Serializable
data class AddHistoryRequest(
    val url: String,
    val title: String,
    val thumbnail: String,
    val duration: Long,
    val channelName: String,
    val channelUrl: String,
    val channelAvatar: String = "",
    val progress: Long = 0L,
)

@Serializable
data class SaveProgressRequest(val position: Long)

@Serializable
data class ProgressItemDto(
    val videoUrl: String = "",
    val position: Long = 0L,
    val updatedAt: Long = 0L,
)

@Serializable
data class SearchHistoryEntryRequest(val query: String)

@Serializable
data class CreatePlaylistRequest(
    val name: String,
    val description: String = "",
)

@Serializable
data class PlaylistReorderRequest(val order: List<String>)

@Serializable
data class AddPlaylistVideoRequest(
    val url: String,
    val title: String,
    val thumbnail: String,
    val duration: Long,
    val channelName: String = "",
    val channelUrl: String = "",
    val channelAvatar: String = "",
    val viewCount: Long = 0L,
)

@Serializable
data class BlockVideoRequest(
    val url: String,
)

@Serializable
data class BlockChannelRequest(
    val url: String,
    val name: String? = null,
    val thumbnailUrl: String? = null,
)

@Serializable
data class BlockedItemDto(
    val url: String,
    val name: String? = null,
    val thumbnailUrl: String? = null,
    val blockedAt: Long = 0,
    val global: Boolean? = null,
)
