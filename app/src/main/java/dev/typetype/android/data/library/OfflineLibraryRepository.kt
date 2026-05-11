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
        playlistsDao.observeIsVideoInPlaylistNamed(FAVORITES_NAME, videoUrl)

    override fun observeIsInWatchLater(url: String): Flow<Boolean> =
        playlistsDao.observeIsVideoInPlaylistNamed(WATCH_LATER_NAME, url)

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

    override suspend fun addFavorite(
        videoUrl: String,
        title: String,
        thumbnail: String,
        duration: Long,
        channelName: String,
        channelUrl: String,
        channelAvatarUrl: String,
        viewCount: Long,
    ): Result<Unit> = runCatching {
        val playlistId = ensureSpecialPlaylist(FAVORITES_NAME)
        network.postAddVideoToPlaylist(
            playlistId = playlistId,
            url = videoUrl,
            title = title,
            thumbnail = thumbnail,
            duration = duration,
            channelName = channelName,
            channelUrl = channelUrl,
            channelAvatar = channelAvatarUrl,
            viewCount = viewCount,
        )
        runCatching { refreshPlaylists() }
    }

    override suspend fun removeFavorite(videoUrl: String): Result<Unit> = runCatching {
        val playlistId = resolveFavoritesPlaylistId()
        playlistId?.let { playlistsDao.deleteVideoFromPlaylist(it, videoUrl) }
        removeFromSpecialPlaylistOrLegacy(
            playlistId = playlistId,
            videoUrl = videoUrl,
            legacyDelete = { network.deleteFavorite(videoUrl) },
        )
    }

    override suspend fun addWatchLater(
        url: String,
        title: String,
        thumbnail: String,
        duration: Long,
        channelName: String,
        channelUrl: String,
        channelAvatarUrl: String,
        viewCount: Long,
    ): Result<Unit> = runCatching {
        val playlistId = ensureSpecialPlaylist(WATCH_LATER_NAME)
        network.postAddVideoToPlaylist(
            playlistId = playlistId,
            url = url,
            title = title,
            thumbnail = thumbnail,
            duration = duration,
            channelName = channelName,
            channelUrl = channelUrl,
            channelAvatar = channelAvatarUrl,
            viewCount = viewCount,
        )
        runCatching { refreshPlaylists() }
    }

    override suspend fun removeWatchLater(url: String): Result<Unit> = runCatching {
        val playlistId = resolveWatchLaterPlaylistId()
        playlistId?.let { playlistsDao.deleteVideoFromPlaylist(it, url) }
        removeFromSpecialPlaylistOrLegacy(
            playlistId = playlistId,
            videoUrl = url,
            legacyDelete = { network.deleteWatchLater(url) },
        )
    }

    private suspend fun ensureSpecialPlaylist(name: String): String {
        playlistsDao.findIdByName(name)?.let { return it }
        runCatching { refreshPlaylists() }
        playlistsDao.findIdByName(name)?.let { return it }
        val id = network.postCreatePlaylist(name)
        runCatching { refreshPlaylists() }
        return id
    }

    private suspend fun resolveFavoritesPlaylistId(): String? =
        playlistsDao.findIdByName(FAVORITES_NAME)
            ?: run {
                runCatching { refreshPlaylists() }
                playlistsDao.findIdByName(FAVORITES_NAME)
            }

    private suspend fun resolveWatchLaterPlaylistId(): String? =
        playlistsDao.findIdByName(WATCH_LATER_NAME)
            ?: run {
                runCatching { refreshPlaylists() }
                playlistsDao.findIdByName(WATCH_LATER_NAME)
            }

    private suspend fun removeFromSpecialPlaylistOrLegacy(
        playlistId: String?,
        videoUrl: String,
        legacyDelete: suspend () -> Unit,
    ) {
        val playlistResult = playlistId?.let { id ->
            runCatching { network.deleteVideoFromPlaylist(id, videoUrl) }
        }
        if (playlistResult?.isSuccess == true) return
        val legacyResult = runCatching { legacyDelete() }
        if (legacyResult.isSuccess) return
        runCatching { refreshPlaylists() }
        val cause = legacyResult.exceptionOrNull()
            ?: playlistResult?.exceptionOrNull()
            ?: error("Remove failed")
        throw cause
    }

    override suspend fun addHistory(
        videoUrl: String,
        title: String,
        thumbnail: String,
        duration: Long,
        channelName: String,
        channelUrl: String,
        channelAvatarUrl: String,
    ): Result<Unit> = runCatching {
        historyDao.deleteByUrl(videoUrl)
        val entity = HistoryEntity(
            id = videoUrl,
            url = videoUrl,
            title = title,
            thumbnailUrl = thumbnail,
            channelName = channelName,
            channelUrl = channelUrl,
            channelAvatarUrl = channelAvatarUrl,
            durationSeconds = duration,
            progressSeconds = historyDao.getProgressSeconds(videoUrl) ?: 0L,
            watchedAtMillis = System.currentTimeMillis(),
        )
        historyDao.upsert(entity)
        runCatching {
            network.postHistory(videoUrl, title, thumbnail, duration, channelName, channelUrl, channelAvatarUrl)
        }
    }

    override suspend fun removeFromHistory(videoUrl: String): Result<Unit> = runCatching {
        historyDao.deleteByUrl(videoUrl)
    }

    override suspend fun clearHistory(): Result<Unit> = runCatching {
        historyDao.replaceAll(emptyList())
        runCatching { network.deleteAllHistory() }
            .onFailure {
                runCatching { refreshHistory() }
                throw it
            }
    }

    override suspend fun saveProgress(videoUrl: String, positionMillis: Long): Result<Unit> = runCatching {
        historyDao.updateProgress(
            url = videoUrl,
            seconds = positionMillis / 1000,
            watchedAtMillis = System.currentTimeMillis(),
        )
        runCatching { network.putProgress(videoUrl, positionMillis) }
    }

    override suspend fun fetchProgressMillis(videoUrl: String): Result<Long?> = runCatching {
        network.getProgress(videoUrl)
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
        channelName: String,
        channelUrl: String,
        channelAvatarUrl: String,
        viewCount: Long,
    ): Result<Unit> = runCatching {
        network.postAddVideoToPlaylist(
            playlistId = playlistId,
            url = videoUrl,
            title = title,
            thumbnail = thumbnail,
            duration = duration,
            channelName = channelName,
            channelUrl = channelUrl,
            channelAvatar = channelAvatarUrl,
            viewCount = viewCount,
        )
        runCatching { refreshPlaylists() }
    }

    override suspend fun removeVideoFromPlaylist(
        playlistId: String,
        videoUrl: String,
    ): Result<Unit> = runCatching {
        playlistsDao.deleteVideoFromPlaylist(playlistId, videoUrl)
        runCatching { network.deleteVideoFromPlaylist(playlistId = playlistId, videoUrl = videoUrl) }
            .onFailure { runCatching { refreshPlaylists() }; throw it }
    }

    private companion object {
        const val FAVORITES_NAME = "Favorites"
        const val WATCH_LATER_NAME = "Watch Later"
    }
}
