package dev.typetype.android.domain.library

data class HistoryItem(
    val id: String,
    val url: String,
    val title: String,
    val thumbnailUrl: String,
    val channelName: String,
    val durationSeconds: Long,
    val progressSeconds: Long,
    val watchedAtMillis: Long,
)

data class WatchLaterItem(
    val url: String,
    val title: String,
    val thumbnailUrl: String,
    val durationSeconds: Long,
    val addedAtMillis: Long,
)

data class FavoriteItem(
    val videoUrl: String,
    val favoritedAtMillis: Long,
)

data class PlaylistVideo(
    val id: String,
    val url: String,
    val title: String,
    val thumbnailUrl: String,
    val durationSeconds: Long,
    val position: Int,
)

data class Playlist(
    val id: String,
    val name: String,
    val description: String,
    val videos: List<PlaylistVideo>,
    val createdAtMillis: Long,
)
