package dev.typetype.android.domain.library

import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun observeHistory(): Flow<List<HistoryItem>>
    fun observeFavorites(): Flow<List<FavoriteItem>>
    fun observeWatchLater(): Flow<List<WatchLaterItem>>
    fun observePlaylists(): Flow<List<Playlist>>
    fun observeIsFavorite(videoUrl: String): Flow<Boolean>
    fun observeIsInWatchLater(url: String): Flow<Boolean>

    suspend fun refreshHistory(): Result<Unit>
    suspend fun refreshFavorites(): Result<Unit>
    suspend fun refreshWatchLater(): Result<Unit>
    suspend fun refreshPlaylists(): Result<Unit>

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

    suspend fun createPlaylist(name: String): Result<String>
    suspend fun addVideoToPlaylist(
        playlistId: String,
        videoUrl: String,
        title: String,
        thumbnail: String,
        duration: Long,
    ): Result<Unit>
}
