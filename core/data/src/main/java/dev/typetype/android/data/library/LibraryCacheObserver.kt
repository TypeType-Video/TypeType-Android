package dev.typetype.android.data.library

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.library.local.FavoritesDao
import dev.typetype.android.data.library.local.HistoryDao
import dev.typetype.android.data.library.local.PlaylistsDao
import dev.typetype.android.data.library.local.WatchLaterDao
import dev.typetype.android.domain.library.FavoriteItem
import dev.typetype.android.domain.library.HistoryItem
import dev.typetype.android.domain.library.HistoryQuery
import dev.typetype.android.domain.library.Playlist
import dev.typetype.android.domain.library.WatchLaterItem
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryCacheObserver @Inject constructor(
    private val activeAccountScope: ActiveAccountScope,
    private val historyDao: HistoryDao,
    private val favoritesDao: FavoritesDao,
    private val watchLaterDao: WatchLaterDao,
    private val playlistsDao: PlaylistsDao,
) {
    fun history(query: HistoryQuery): Flow<PagingData<HistoryItem>> =
        activeAccountScope.observe().flatMapLatest { scope ->
            if (scope == null) {
                flowOf(PagingData.empty())
            } else {
                Pager(
                    config = PagingConfig(
                        pageSize = HISTORY_PAGE_SIZE,
                        initialLoadSize = HISTORY_INITIAL_LOAD_SIZE,
                        prefetchDistance = HISTORY_PREFETCH_DISTANCE,
                        maxSize = HISTORY_MAX_LOADED_ITEMS,
                        enablePlaceholders = false,
                    ),
                    pagingSourceFactory = {
                        historyDao.pagingSource(
                            serverId = scope.serverId,
                            accountId = scope.accountId,
                            search = query.search.trim(),
                            orderKey = query.order.storageKey,
                            fromMillis = query.fromMillis,
                            toMillis = query.toMillis,
                        )
                    },
                ).flow
            }
        }.map { page -> page.map { it.toDomain() } }

    fun historyCount(): Flow<Int> = activeAccountScope.observe().flatMapLatest { scope ->
        if (scope == null) flowOf(0)
        else historyDao.observeCount(scope.serverId, scope.accountId)
    }

    fun watchedUrls(): Flow<Set<String>> = activeAccountScope.observe().flatMapLatest { scope ->
        if (scope == null) flowOf(emptySet())
        else historyDao.observeWatchedUrls(scope.serverId, scope.accountId).map { it.toSet() }
    }

    fun continueWatching(limit: Int): Flow<List<HistoryItem>> =
        activeAccountScope.observe().flatMapLatest { scope ->
            if (scope == null) {
                flowOf(emptyList())
            } else {
                historyDao.observeContinueWatching(scope.serverId, scope.accountId, limit)
                    .map { rows -> rows.map { it.toDomain() } }
            }
        }

    fun favorites(): Flow<List<FavoriteItem>> = scoped { serverId, accountId ->
        favoritesDao.observeAll(serverId, accountId)
    }.map { rows -> rows.map { it.toDomain() } }

    fun watchLater(): Flow<List<WatchLaterItem>> = scoped { serverId, accountId ->
        watchLaterDao.observeAll(serverId, accountId)
    }.map { rows -> rows.map { it.toDomain() } }

    fun playlists(): Flow<List<Playlist>> = scoped { serverId, accountId ->
        playlistsDao.observeAllWithVideos(serverId, accountId)
    }.map { rows -> rows.map { it.toDomain() } }

    fun favoriteMembership(videoUrl: String): Flow<Boolean> =
        activeAccountScope.observe().flatMapLatest { scope ->
            if (scope == null) flowOf(false)
            else favoritesDao.observeIsFavorite(
                scope.serverId,
                scope.accountId,
                videoUrl,
            )
        }

    fun watchLaterMembership(videoUrl: String): Flow<Boolean> =
        activeAccountScope.observe().flatMapLatest { scope ->
            if (scope == null) flowOf(false)
            else watchLaterDao.observeIsInWatchLater(scope.serverId, scope.accountId, videoUrl)
        }

    private fun <T> scoped(source: (String, String) -> Flow<List<T>>): Flow<List<T>> =
        activeAccountScope.observe().flatMapLatest { scope ->
            if (scope == null) flowOf(emptyList()) else source(scope.serverId, scope.accountId)
        }

    private companion object {
        const val HISTORY_PAGE_SIZE = 30
        const val HISTORY_INITIAL_LOAD_SIZE = 60
        const val HISTORY_PREFETCH_DISTANCE = 8
        const val HISTORY_MAX_LOADED_ITEMS = 180
    }
}
