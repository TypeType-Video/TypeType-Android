package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class AddFavoriteRequest(val videoUrl: String)

@Serializable
data class AddWatchLaterRequest(
    val url: String,
    val title: String,
    val thumbnail: String,
    val duration: Long,
)

@Serializable
data class AddHistoryRequest(
    val videoUrl: String,
    val title: String,
    val thumbnail: String,
    val duration: Long,
    val channelName: String,
    val channelUrl: String,
)

@Serializable
data class SaveProgressRequest(val url: String, val positionMillis: Long)

@Serializable
data class SearchHistoryEntryRequest(val query: String)
