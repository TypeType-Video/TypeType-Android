package dev.typetype.android.data.library

import dev.typetype.android.data.library.local.FavoriteEntity
import dev.typetype.android.data.library.local.FavoritesDao
import dev.typetype.android.data.library.local.HistoryDao
import dev.typetype.android.data.library.local.HistoryEntity
import dev.typetype.android.data.library.local.PlaylistsDao
import dev.typetype.android.data.library.local.WatchLaterDao
import dev.typetype.android.data.library.local.WatchLaterEntity
import dev.typetype.android.domain.library.FavoriteItem
import dev.typetype.android.domain.library.HistoryItem
import dev.typetype.android.domain.library.LibraryRepository
import dev.typetype.android.domain.library.Playlist
import dev.typetype.android.domain.library.WatchLaterItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class OfflineLibraryRepository @Inject constructor(
    private val favoritesDao: FavoritesDao,
    private val historyDao: HistoryDao,
    private val watchLaterDao: WatchLaterDao,
    private val playlistsDao: PlaylistsDao,
    private val network: LibraryNetworkSource,
) : LibraryRepository {

    override fun observeHistory(): Flow<List<HistoryItem>> =
        historyDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeFavorites(): Flow<List<FavoriteItem>> =
        favoritesDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeWatchLater(): Flow<List<WatchLaterItem>> =
        watchLaterDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observePlaylists(): Flow<List<Playlist>> =
        playlistsDao.observeAllWithVideos().map { rows -> rows.map { it.toDomain() } }

    override fun observeIsFavorite(videoUrl: String): Flow<Boolean> =
        favoritesDao.observeIsFavorite(videoUrl)

    override fun observeIsInWatchLater(url: String): Flow<Boolean> =
        watchLaterDao.observeIsInWatchLater(url)

    override suspend fun refreshHistory(): Result<Unit> = runCatching {
        historyDao.replaceAll(network.fetchHistory())
    }

    override suspend fun refreshFavorites(): Result<Unit> = runCatching {
        favoritesDao.replaceAll(network.fetchFavorites())
    }

    override suspend fun refreshWatchLater(): Result<Unit> = runCatching {
        watchLaterDao.replaceAll(network.fetchWatchLater())
    }

    override suspend fun refreshPlaylists(): Result<Unit> = runCatching {
        val (playlists, videos) = network.fetchPlaylists()
        playlistsDao.replaceAll(playlists, videos)
    }

    override suspend fun addFavorite(videoUrl: String): Result<Unit> = runCatching {
        val entity = FavoriteEntity(videoUrl = videoUrl, favoritedAtMillis = System.currentTimeMillis())
        favoritesDao.upsert(entity)
        runCatching { network.postFavorite(videoUrl) }
            .onFailure {
                favoritesDao.deleteByUrl(videoUrl)
                throw it
            }
    }

    override suspend fun removeFavorite(videoUrl: String): Result<Unit> = runCatching {
        favoritesDao.deleteByUrl(videoUrl)
        runCatching { network.deleteFavorite(videoUrl) }
            .onFailure { throw it }
    }

    override suspend fun addWatchLater(
        url: String,
        title: String,
        thumbnail: String,
        duration: Long,
    ): Result<Unit> = runCatching {
        val entity = WatchLaterEntity(
            url = url,
            title = title,
            thumbnailUrl = thumbnail,
            durationSeconds = duration,
            addedAtMillis = System.currentTimeMillis(),
        )
        watchLaterDao.upsert(entity)
        runCatching { network.postWatchLater(url, title, thumbnail, duration) }
            .onFailure {
                watchLaterDao.deleteByUrl(url)
                throw it
            }
    }

    override suspend fun removeWatchLater(url: String): Result<Unit> = runCatching {
        watchLaterDao.deleteByUrl(url)
        runCatching { network.deleteWatchLater(url) }
            .onFailure { throw it }
    }

    override suspend fun addHistory(
        videoUrl: String,
        title: String,
        thumbnail: String,
        duration: Long,
        channelName: String,
        channelUrl: String,
    ): Result<Unit> = runCatching {
        // The DB primary key is `id`, so a previously refreshed row from the
        // server (id = <server-id>) and a freshly added local row (id = url)
        // can coexist for the same URL. Wipe any existing rows for this URL
        // before inserting so the displayed list never duplicates.
        historyDao.deleteByUrl(videoUrl)
        val entity = HistoryEntity(
            id = videoUrl,
            url = videoUrl,
            title = title,
            thumbnailUrl = thumbnail,
            channelName = channelName,
            durationSeconds = duration,
            progressSeconds = historyDao.getProgressSeconds(videoUrl) ?: 0L,
            watchedAtMillis = System.currentTimeMillis(),
        )
        historyDao.upsert(entity)
        runCatching { network.postHistory(videoUrl, title, thumbnail, duration, channelName, channelUrl) }
    }

    override suspend fun saveProgress(videoUrl: String, positionMillis: Long): Result<Unit> = runCatching {
        historyDao.updateProgress(
            url = videoUrl,
            seconds = positionMillis / 1000,
            watchedAtMillis = System.currentTimeMillis(),
        )
        runCatching { network.putProgress(videoUrl, positionMillis) }
    }

    override suspend fun createPlaylist(name: String): Result<String> = runCatching {
        val playlistId = network.postCreatePlaylist(name)
        runCatching { refreshPlaylists() }
        playlistId
    }

    override suspend fun addVideoToPlaylist(
        playlistId: String,
        videoUrl: String,
        title: String,
        thumbnail: String,
        duration: Long,
    ): Result<Unit> = runCatching {
        network.postAddVideoToPlaylist(
            playlistId = playlistId,
            url = videoUrl,
            title = title,
            thumbnail = thumbnail,
            duration = duration,
        )
        runCatching { refreshPlaylists() }
    }
}
