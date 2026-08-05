package dev.typetype.android.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class YoutubeTakeoutCommitRequestDto(
    val importSubscriptions: Boolean,
    val importPlaylists: Boolean,
    val importPlaylistItems: Boolean,
    val importFavorites: Boolean,
    val importWatchLater: Boolean,
    val importHistory: Boolean,
)
