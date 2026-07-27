package dev.typetype.android.data.library

import androidx.paging.PagingData
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.library.local.HistoryDao
import dev.typetype.android.data.library.local.PlaylistEntity
import dev.typetype.android.data.library.local.PlaylistsDao
import dev.typetype.android.data.library.sync.LibrarySyncTracker
import dev.typetype.android.domain.library.FavoriteItem
import dev.typetype.android.domain.library.HistoryItem
import dev.typetype.android.domain.library.HistoryQuery
import dev.typetype.android.domain.library.LibraryCollection
import dev.typetype.android.domain.library.LibraryCollectionSyncState
import dev.typetype.android.domain.library.LibraryRepository
import dev.typetype.android.domain.library.Playlist
import dev.typetype.android.domain.library.WatchLaterItem
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineLibraryRepository @Inject constructor(
    private val historyDao: HistoryDao,
    private val playlistsDao: PlaylistsDao,
    private val network: LibraryNetworkSource,
    private val activeAccountScope: ActiveAccountScope,
    private val cacheObserver: LibraryCacheObserver,
    private val progressSync: LibraryProgressSync,
    private val refreshCoordinator: LibraryRefreshCoordinator,
    private val syncTracker: LibrarySyncTracker,
    private val mutations: LibraryOptimisticMutations,
    private val userSettingsRepository: UserSettingsRepository,
) : LibraryRepository {

    override fun observeHistory(query: HistoryQuery): Flow<PagingData<HistoryItem>> =
        cacheObserver.history(query)

    override fun observeHistoryCount(): Flow<Int> = combine(
        cacheObserver.historyCount(),
        refreshCoordinator.observeHistoryTotal(),
    ) { cached, total -> total ?: cached }

    override fun observeWatchedUrls(): Flow<Set<String>> = cacheObserver.watchedUrls()

    override fun observeContinueWatching(limit: Int): Flow<List<HistoryItem>> =
        cacheObserver.continueWatching(limit)

    override fun observeFavorites(): Flow<List<FavoriteItem>> = cacheObserver.favorites()

    override fun observeWatchLater(): Flow<List<WatchLaterItem>> = cacheObserver.watchLater()

    override fun observePlaylists(): Flow<List<Playlist>> = cacheObserver.playlists()

    override fun observeSyncState(): Flow<Map<LibraryCollection, LibraryCollectionSyncState>> =
        syncTracker.observe()

    override fun observeIsFavorite(videoUrl: String): Flow<Boolean> =
        cacheObserver.favoriteMembership(videoUrl)

    override fun observeIsInWatchLater(url: String): Flow<Boolean> =
        cacheObserver.watchLaterMembership(url)

    override suspend fun refreshHistory(): Result<Unit> = refreshCoordinator.history()

    override suspend fun loadMoreHistory(): Result<Boolean> = refreshCoordinator.loadMoreHistory()

    override suspend fun refreshFavorites(): Result<Unit> = refreshCoordinator.favorites()

    override suspend fun refreshWatchLater(): Result<Unit> = refreshCoordinator.watchLater()

    override suspend fun refreshPlaylists(): Result<Unit> = refreshCoordinator.playlists()

    override suspend fun refreshPlaylist(playlistId: String): Result<Unit> =
        refreshCoordinator.playlist(playlistId)

    override suspend fun retryPendingWrites(collection: LibraryCollection): Result<Boolean> =
        runCatching { mutations.retry(collection) }

    override suspend fun resumePendingWrites(): Result<Boolean> = runCatching { mutations.resume() }

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
        mutations.favorite(
            MutationVideo(
                url = videoUrl,
                title = title,
                thumbnailUrl = thumbnail,
                durationSeconds = duration,
                channelName = channelName,
                channelUrl = channelUrl,
                channelAvatarUrl = channelAvatarUrl,
                viewCount = viewCount,
            ),
            desiredPresent = true,
        )
    }

    override suspend fun removeFavorite(videoUrl: String): Result<Unit> = runCatching {
        mutations.favorite(MutationVideo(videoUrl), desiredPresent = false)
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
        mutations.watchLater(
            MutationVideo(
                url = url,
                title = title,
                thumbnailUrl = thumbnail,
                durationSeconds = duration,
                channelName = channelName,
                channelUrl = channelUrl,
                channelAvatarUrl = channelAvatarUrl,
                viewCount = viewCount,
            ),
            desiredPresent = true,
        )
    }

    override suspend fun removeWatchLater(url: String): Result<Unit> = runCatching {
        mutations.watchLater(MutationVideo(url), desiredPresent = false)
    }

    override suspend fun addHistory(
        videoUrl: String,
        title: String,
        thumbnail: String,
        duration: Long,
        channelName: String,
        channelUrl: String,
        channelAvatarUrl: String,
    ): Result<Unit> = captureLibraryResult {
        if (userSettingsRepository.current().getOrThrow().disableWatchHistory) {
            return@captureLibraryResult
        }
        val scope = activeAccountScope.require()
        val progress = historyDao.getProgressSeconds(scope.serverId, scope.accountId, videoUrl) ?: 0L
        val confirmed = network.postHistory(
            scope,
            videoUrl,
            title,
            thumbnail,
            duration,
            channelName,
            channelUrl,
            channelAvatarUrl,
        )
        if (confirmed != null) {
            historyDao.deleteByUrl(scope.serverId, scope.accountId, videoUrl)
            historyDao.upsert(confirmed.copy(progressSeconds = maxOf(progress, confirmed.progressSeconds)))
            refreshCoordinator.recordHistoryAdded(scope)
        }
    }

    override suspend fun removeFromHistory(videoUrl: String): Result<Unit> = runCatching {
        val scope = activeAccountScope.require()
        val id = historyDao.getIdByUrl(scope.serverId, scope.accountId, videoUrl)
        if (id != null) network.deleteHistory(scope, id)
        historyDao.deleteByUrl(scope.serverId, scope.accountId, videoUrl)
        if (id != null) refreshCoordinator.recordHistoryRemoved(scope)
    }

    override suspend fun clearHistory(): Result<Unit> = runCatching {
        val scope = activeAccountScope.require()
        network.deleteAllHistory(scope)
        historyDao.replaceAll(scope.serverId, scope.accountId, emptyList())
        refreshCoordinator.recordHistoryCleared(scope)
    }

    override suspend fun saveProgress(videoUrl: String, positionMillis: Long): Result<Unit> = captureLibraryResult {
        if (userSettingsRepository.current().getOrThrow().disableWatchHistory) {
            return@captureLibraryResult
        }
        val scope = activeAccountScope.require()
        progressSync.save(scope, videoUrl, positionMillis)
    }

    override suspend fun discardPendingProgress(): Result<Unit> = captureLibraryResult {
        progressSync.discardPending(activeAccountScope.require())
    }

    override suspend fun fetchProgressMillis(videoUrl: String): Result<Long?> = runCatching {
        val scope = activeAccountScope.require()
        progressSync.fetch(scope, videoUrl)
    }

    override suspend fun createPlaylist(name: String): Result<String> = runCatching {
        val scope = activeAccountScope.require()
        val playlist = network.postCreatePlaylist(scope, name)
        mutations.recordCreatedPlaylist(playlist)
        playlist.id
    }

    override suspend fun renamePlaylist(playlistId: String, name: String): Result<Unit> = runCatching {
        val scope = activeAccountScope.require()
        val cacheKey = PlaylistEntity.cacheKey(scope, playlistId)
        val playlist = checkNotNull(playlistsDao.getPlaylist(cacheKey)) { "Playlist not found" }
        network.putPlaylist(scope, playlistId, name, playlist.description)
        mutations.recordRenamedPlaylist(playlistId, name)
    }

    override suspend fun deletePlaylist(playlistId: String): Result<Unit> = runCatching {
        val scope = activeAccountScope.require()
        network.deletePlaylist(scope, playlistId)
        mutations.recordDeletedPlaylist(playlistId)
    }

    override suspend fun reorderPlaylist(
        playlistId: String,
        orderedVideoUrls: List<String>,
    ): Result<Unit> = runCatching {
        val scope = activeAccountScope.require()
        val cacheKey = PlaylistEntity.cacheKey(scope, playlistId)
        val cachedUrls = playlistsDao.getVideoUrls(cacheKey)
        require(orderedVideoUrls.size == orderedVideoUrls.distinct().size) {
            "Playlist order contains duplicate videos"
        }
        require(orderedVideoUrls.toSet() == cachedUrls.toSet()) {
            "Playlist order does not match cached videos"
        }
        network.putPlaylistOrder(scope, playlistId, orderedVideoUrls)
        playlistsDao.reorderVideos(cacheKey, orderedVideoUrls)
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
        mutations.playlistVideo(
            playlistId = playlistId,
            video = MutationVideo(
                url = videoUrl,
                title = title,
                thumbnailUrl = thumbnail,
                durationSeconds = duration,
                channelName = channelName,
                channelUrl = channelUrl,
                channelAvatarUrl = channelAvatarUrl,
                viewCount = viewCount,
            ),
            desiredPresent = true,
        )
    }

    override suspend fun removeVideoFromPlaylist(
        playlistId: String,
        videoUrl: String,
    ): Result<Unit> = runCatching {
        mutations.playlistVideo(
            playlistId = playlistId,
            video = MutationVideo(videoUrl),
            desiredPresent = false,
        )
    }
}

private suspend fun <T> captureLibraryResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Throwable) {
    Result.failure(failure)
}
