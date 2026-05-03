package dev.typetype.android.data.library

import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.domain.library.FavoriteItem
import dev.typetype.android.domain.library.HistoryItem
import dev.typetype.android.domain.library.LibraryRepository
import dev.typetype.android.domain.library.Playlist
import dev.typetype.android.domain.library.PlaylistVideo
import dev.typetype.android.domain.library.WatchLaterItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
) : LibraryRepository {

    override suspend fun loadHistory(): Result<List<HistoryItem>> = runCatching {
        val api = apiHolder.require()
        val response = withContext(Dispatchers.IO) { api.history() }
        if (!response.isSuccessful) error("History failed (HTTP ${response.code()})")
        (response.body() ?: emptyList()).map { dto ->
            HistoryItem(
                id = dto.id,
                url = dto.url,
                title = dto.title,
                thumbnailUrl = dto.thumbnail,
                channelName = dto.channelName,
                durationSeconds = dto.duration,
                progressSeconds = dto.progress,
                watchedAtMillis = dto.watchedAt,
            )
        }
    }

    override suspend fun loadFavorites(): Result<List<FavoriteItem>> = runCatching {
        val api = apiHolder.require()
        val response = withContext(Dispatchers.IO) { api.favorites() }
        if (!response.isSuccessful) error("Favorites failed (HTTP ${response.code()})")
        (response.body() ?: emptyList()).map { dto ->
            FavoriteItem(videoUrl = dto.videoUrl, favoritedAtMillis = dto.favoritedAt)
        }
    }

    override suspend fun loadWatchLater(): Result<List<WatchLaterItem>> = runCatching {
        val api = apiHolder.require()
        val response = withContext(Dispatchers.IO) { api.watchLater() }
        if (!response.isSuccessful) error("Watch later failed (HTTP ${response.code()})")
        (response.body() ?: emptyList()).map { dto ->
            WatchLaterItem(
                url = dto.url,
                title = dto.title,
                thumbnailUrl = dto.thumbnail,
                durationSeconds = dto.duration,
                addedAtMillis = dto.addedAt,
            )
        }
    }

    override suspend fun loadPlaylists(): Result<List<Playlist>> = runCatching {
        val api = apiHolder.require()
        val response = withContext(Dispatchers.IO) { api.playlists() }
        if (!response.isSuccessful) error("Playlists failed (HTTP ${response.code()})")
        (response.body() ?: emptyList()).map { dto ->
            Playlist(
                id = dto.id,
                name = dto.name,
                description = dto.description,
                createdAtMillis = dto.createdAt,
                videos = dto.videos.map { v ->
                    PlaylistVideo(
                        id = v.id,
                        url = v.url,
                        title = v.title,
                        thumbnailUrl = v.thumbnail,
                        durationSeconds = v.duration,
                        position = v.position,
                    )
                },
            )
        }
    }
}
