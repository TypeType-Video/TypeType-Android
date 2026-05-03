package dev.typetype.android.domain.library

interface LibraryRepository {
    suspend fun loadHistory(): Result<List<HistoryItem>>
    suspend fun loadFavorites(): Result<List<FavoriteItem>>
    suspend fun loadWatchLater(): Result<List<WatchLaterItem>>
    suspend fun loadPlaylists(): Result<List<Playlist>>
}
