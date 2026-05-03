package dev.typetype.android.domain.library

interface LibraryRepository {
    suspend fun loadHistory(): Result<List<HistoryItem>>
    suspend fun loadFavorites(): Result<List<FavoriteItem>>
    suspend fun loadWatchLater(): Result<List<WatchLaterItem>>
    suspend fun loadPlaylists(): Result<List<Playlist>>
    suspend fun addFavorite(videoUrl: String): Result<Unit>
    suspend fun removeFavorite(videoUrl: String): Result<Unit>
    suspend fun addWatchLater(url: String, title: String, thumbnail: String, duration: Long): Result<Unit>
    suspend fun removeWatchLater(url: String): Result<Unit>
    suspend fun addHistory(
        videoUrl: String,
        title: String,
        thumbnail: String,
        duration: Long,
        channelName: String,
        channelUrl: String,
    ): Result<Unit>
    suspend fun saveProgress(videoUrl: String, positionMillis: Long): Result<Unit>
}
