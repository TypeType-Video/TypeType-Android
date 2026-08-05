package dev.typetype.android.data.library

import androidx.room.withTransaction
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.database.TypeTypeDatabase
import dev.typetype.android.data.library.local.FavoritesDao
import dev.typetype.android.data.library.local.FavoriteEntity
import dev.typetype.android.data.library.local.HistoryDao
import dev.typetype.android.data.library.local.PlaylistsDao
import dev.typetype.android.data.library.local.WatchLaterDao
import dev.typetype.android.data.library.local.WatchLaterEntity
import dev.typetype.android.data.library.sync.LibraryMutationOverlay
import dev.typetype.android.data.library.sync.LibraryRefreshToken
import dev.typetype.android.data.library.sync.LibrarySyncTracker
import dev.typetype.android.domain.library.LibraryCollection
import dev.typetype.android.domain.library.HistoryQuery
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LibraryRefreshCoordinator @Inject constructor(
    private val network: LibraryNetworkSource,
    private val activeAccountScope: ActiveAccountScope,
    private val tracker: LibrarySyncTracker,
    private val historyDao: HistoryDao,
    private val favoritesDao: FavoritesDao,
    private val watchLaterDao: WatchLaterDao,
    private val playlistsDao: PlaylistsDao,
    private val mutationOverlay: LibraryMutationOverlay,
    private val database: TypeTypeDatabase,
) {
    private val historyPageMutex = Mutex()
    private var historyPageScope: AccountScope? = null
    private var historyPageQuery = HistoryQuery()
    private var nextHistoryOffset = 0
    private var hasMoreHistory = false
    private val historyTotal = MutableStateFlow<ScopedHistoryTotal?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeHistoryTotal(): Flow<Int?> = activeAccountScope.observe().flatMapLatest { scope ->
        if (scope == null) flowOf(null)
        else historyTotal.map { total -> total?.takeIf { it.scope == scope }?.count }
    }

    fun recordHistoryAdded(scope: AccountScope) = adjustHistoryTotal(scope, 1)

    fun recordHistoryRemoved(scope: AccountScope) = adjustHistoryTotal(scope, -1)

    fun recordHistoryCleared(scope: AccountScope) {
        historyTotal.value = ScopedHistoryTotal(scope, 0)
    }

    suspend fun history(query: HistoryQuery = HistoryQuery()): Result<Unit> = historyPageMutex.withLock {
        historyPageScope = null
        nextHistoryOffset = 0
        hasMoreHistory = false
        refresh(
            collection = LibraryCollection.History,
            load = { scope -> network.fetchHistory(scope, query) },
            updateCache = { scope, page ->
                if (query.hasRemoteFilter) {
                    historyDao.deleteMatching(
                        scope.serverId,
                        scope.accountId,
                        query.search.trim(),
                        query.fromMillis,
                        query.toMillis,
                    )
                    historyDao.upsertAll(page.rows)
                } else {
                    historyDao.replaceAll(scope.serverId, scope.accountId, page.rows)
                }
                historyPageScope = scope
                historyPageQuery = query
                nextHistoryOffset = page.nextOffset
                hasMoreHistory = !query.hasRemoteFilter && page.hasMore
                if (!query.hasRemoteFilter) {
                    historyTotal.value = ScopedHistoryTotal(scope, page.totalCount)
                }
            },
        )
    }

    suspend fun loadMoreHistory(query: HistoryQuery = HistoryQuery()): Result<Boolean> =
        historyPageMutex.withLock {
        val scope = resultPreservingCancellation { activeAccountScope.require() }
            .getOrElse { return@withLock Result.failure(it) }
        if (historyPageScope != scope || historyPageQuery != query || !hasMoreHistory) {
            return@withLock Result.success(false)
        }
        try {
            val page = network.fetchHistory(scope, query = query, offset = nextHistoryOffset)
            activeAccountScope.verify(scope)
            database.withTransaction {
                historyDao.upsertAll(page.rows)
            }
            nextHistoryOffset = page.nextOffset
            hasMoreHistory = page.hasMore
            historyTotal.value = ScopedHistoryTotal(scope, page.totalCount)
            Result.success(hasMoreHistory)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            Result.failure(failure)
        }
    }

    suspend fun favorites(): Result<Unit> = refresh(
        collection = LibraryCollection.Favorites,
        load = network::fetchFavorites,
        updateCache = { scope, rows ->
            val existing = favoritesDao.getAll(scope.serverId, scope.accountId).associateBy { it.videoUrl }
            favoritesDao.replaceAll(
                scope.serverId,
                scope.accountId,
                rows.map { it.withMetadataFallback(existing[it.videoUrl]) },
            )
        },
    )

    suspend fun watchLater(): Result<Unit> = refresh(
        collection = LibraryCollection.WatchLater,
        load = network::fetchWatchLater,
        updateCache = { scope, rows ->
            val existing = watchLaterDao.getAll(scope.serverId, scope.accountId).associateBy { it.url }
            watchLaterDao.replaceAll(
                scope.serverId,
                scope.accountId,
                rows.map { it.withMetadataFallback(existing[it.url]) },
            )
        },
    )

    suspend fun playlists(): Result<Unit> = refresh(
        collection = LibraryCollection.Playlists,
        load = network::fetchPlaylistSummaries,
        updateCache = { scope, rows ->
            playlistsDao.replaceSummaries(scope.serverId, scope.accountId, rows)
        },
    )

    suspend fun playlist(playlistId: String): Result<Unit> {
        val scope = resultPreservingCancellation { activeAccountScope.require() }
            .getOrElse { return Result.failure(it) }
        return try {
            val (playlist, videos) = network.fetchPlaylist(scope, playlistId)
            activeAccountScope.verify(scope)
            database.withTransaction {
                playlistsDao.replaceDetail(playlist, videos)
                mutationOverlay.apply(scope, LibraryCollection.Playlists)
            }
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            Result.failure(failure)
        }
    }

    private suspend fun <T> refresh(
        collection: LibraryCollection,
        load: suspend (AccountScope) -> T,
        updateCache: suspend (AccountScope, T) -> Unit,
    ): Result<Unit> {
        val scope = resultPreservingCancellation { activeAccountScope.require() }
            .getOrElse { return Result.failure(it) }
        val token = resultPreservingCancellation { tracker.begin(scope, collection) }
            .getOrElse { return Result.failure(it) }
        return try {
            val rows = load(scope)
            activeAccountScope.verify(scope)
            database.withTransaction {
                if (!tracker.isCurrent(token)) throw SupersededLibraryRefresh()
                updateCache(scope, rows)
                mutationOverlay.apply(scope, collection)
                tracker.succeed(token)
            }
            Result.success(Unit)
        } catch (failure: CancellationException) {
            throw failure
        } catch (failure: Throwable) {
            recordCurrentFailure(token, failure)
            Result.failure(failure)
        }
    }

    private suspend fun recordCurrentFailure(token: LibraryRefreshToken, failure: Throwable) {
        val current = try {
            activeAccountScope.verify(token.scope)
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            false
        }
        if (current) {
            try {
                tracker.fail(token, failure)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (trackingFailure: Throwable) {
                failure.addSuppressed(trackingFailure)
            }
        }
    }

    private suspend fun <T> resultPreservingCancellation(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            Result.failure(failure)
        }

    private fun adjustHistoryTotal(scope: AccountScope, delta: Int) {
        val current = historyTotal.value?.takeIf { it.scope == scope } ?: return
        historyTotal.value = current.copy(count = maxOf(0, current.count + delta))
    }
}

private fun FavoriteEntity.withMetadataFallback(existing: FavoriteEntity?): FavoriteEntity = copy(
    title = title.ifBlank { existing?.title.orEmpty() },
    thumbnailUrl = thumbnailUrl.ifBlank { existing?.thumbnailUrl.orEmpty() },
    durationSeconds = durationSeconds.takeIf { it > 0L } ?: existing?.durationSeconds ?: 0L,
    channelName = channelName.ifBlank { existing?.channelName.orEmpty() },
    channelUrl = channelUrl.ifBlank { existing?.channelUrl.orEmpty() },
    channelAvatarUrl = channelAvatarUrl.ifBlank { existing?.channelAvatarUrl.orEmpty() },
    viewCount = viewCount.takeIf { it > 0L } ?: existing?.viewCount ?: 0L,
)

private fun WatchLaterEntity.withMetadataFallback(existing: WatchLaterEntity?): WatchLaterEntity = copy(
    title = title.ifBlank { existing?.title.orEmpty() },
    thumbnailUrl = thumbnailUrl.ifBlank { existing?.thumbnailUrl.orEmpty() },
    durationSeconds = durationSeconds.takeIf { it > 0L } ?: existing?.durationSeconds ?: 0L,
    channelName = channelName.ifBlank { existing?.channelName.orEmpty() },
    channelUrl = channelUrl.ifBlank { existing?.channelUrl.orEmpty() },
    channelAvatarUrl = channelAvatarUrl.ifBlank { existing?.channelAvatarUrl.orEmpty() },
    viewCount = viewCount.takeIf { it > 0L } ?: existing?.viewCount ?: 0L,
)

private class SupersededLibraryRefresh : CancellationException("Library refresh was superseded")

private data class ScopedHistoryTotal(val scope: AccountScope, val count: Int)
