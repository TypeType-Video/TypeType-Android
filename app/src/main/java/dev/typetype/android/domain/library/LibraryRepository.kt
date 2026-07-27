package dev.typetype.android.domain.library

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun observeHistory(query: HistoryQuery): Flow<PagingData<HistoryItem>>
    fun observeHistoryCount(): Flow<Int>
    fun observeWatchedUrls(): Flow<Set<String>>
    fun observeContinueWatching(limit: Int): Flow<List<HistoryItem>>
    fun observeFavorites(): Flow<List<FavoriteItem>>
    fun observeWatchLater(): Flow<List<WatchLaterItem>>
    fun observePlaylists(): Flow<List<Playlist>>
    fun observeSyncState(): Flow<Map<LibraryCollection, LibraryCollectionSyncState>>
    fun observeIsFavorite(videoUrl: String): Flow<Boolean>
    fun observeIsInWatchLater(url: String): Flow<Boolean>

    suspend fun refreshHistory(): Result<Unit>
    suspend fun loadMoreHistory(): Result<Boolean>
    suspend fun refreshFavorites(): Result<Unit>
    suspend fun refreshWatchLater(): Result<Unit>
    suspend fun refreshPlaylists(): Result<Unit>
    suspend fun refreshPlaylist(playlistId: String): Result<Unit>
    suspend fun retryPendingWrites(collection: LibraryCollection): Result<Boolean>
    suspend fun resumePendingWrites(): Result<Boolean>

    suspend fun addFavorite(
        videoUrl: String,
        title: String,
        thumbnail: String,
        duration: Long,
        channelName: String = "",
        channelUrl: String = "",
        channelAvatarUrl: String = "",
        viewCount: Long = 0L,
    ): Result<Unit>
    suspend fun removeFavorite(videoUrl: String): Result<Unit>
    suspend fun addWatchLater(
        url: String,
        title: String,
        thumbnail: String,
        duration: Long,
        channelName: String = "",
        channelUrl: String = "",
        channelAvatarUrl: String = "",
        viewCount: Long = 0L,
    ): Result<Unit>
    suspend fun removeWatchLater(url: String): Result<Unit>
    suspend fun addHistory(
        videoUrl: String,
        title: String,
        thumbnail: String,
        duration: Long,
        channelName: String,
        channelUrl: String,
        channelAvatarUrl: String = "",
    ): Result<Unit>
    suspend fun saveProgress(videoUrl: String, positionMillis: Long): Result<Unit>
    suspend fun discardPendingProgress(): Result<Unit>
    suspend fun fetchProgressMillis(videoUrl: String): Result<Long?>
    suspend fun removeFromHistory(videoUrl: String): Result<Unit>
    suspend fun clearHistory(): Result<Unit>

    suspend fun createPlaylist(name: String): Result<String>
    suspend fun renamePlaylist(playlistId: String, name: String): Result<Unit>
    suspend fun deletePlaylist(playlistId: String): Result<Unit>
    suspend fun reorderPlaylist(playlistId: String, orderedVideoUrls: List<String>): Result<Unit>
    suspend fun addVideoToPlaylist(
        playlistId: String,
        videoUrl: String,
        title: String,
        thumbnail: String,
        duration: Long,
        channelName: String = "",
        channelUrl: String = "",
        channelAvatarUrl: String = "",
        viewCount: Long = 0L,
    ): Result<Unit>
    suspend fun removeVideoFromPlaylist(playlistId: String, videoUrl: String): Result<Unit>
}
