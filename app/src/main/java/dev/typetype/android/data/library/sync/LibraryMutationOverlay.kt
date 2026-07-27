package dev.typetype.android.data.library.sync

import dev.typetype.android.data.account.AccountDao
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.library.local.FavoriteEntity
import dev.typetype.android.data.library.local.FavoritesDao
import dev.typetype.android.data.library.local.PlaylistEntity
import dev.typetype.android.data.library.local.PlaylistVideoEntity
import dev.typetype.android.data.library.local.PlaylistsDao
import dev.typetype.android.data.library.local.WatchLaterDao
import dev.typetype.android.data.library.local.WatchLaterEntity
import dev.typetype.android.data.subscriptions.SubscriptionDao
import dev.typetype.android.data.subscriptions.SubscriptionEntity
import dev.typetype.android.domain.library.LibraryCollection
import javax.inject.Inject

class LibraryMutationOverlay @Inject constructor(
    private val accountDao: AccountDao,
    private val mutationDao: LibraryMutationDao,
    private val favoritesDao: FavoritesDao,
    private val watchLaterDao: WatchLaterDao,
    private val playlistsDao: PlaylistsDao,
    private val subscriptionDao: SubscriptionDao,
) {
    suspend fun apply(scope: AccountScope, collection: LibraryCollection) {
        val generation = accountDao.get(scope.serverId, scope.accountId)?.sessionGeneration ?: return
        val rows = mutationDao.forCollection(scope.serverId, scope.accountId, collection.storageKey)
            .filter { it.sessionGeneration == generation }
            .sortedBy { it.updatedAtMillis }
        rows.forEach { row -> apply(scope, row) }
    }

    private suspend fun apply(scope: AccountScope, row: LibraryMutationEntity) {
        when (row.kindOrThrow()) {
            LibraryMutationKind.Favorite -> applyFavorite(scope, row)
            LibraryMutationKind.WatchLater -> applyWatchLater(scope, row)
            LibraryMutationKind.Subscription -> applySubscription(scope, row)
            LibraryMutationKind.PlaylistVideo -> applyPlaylistVideo(scope, row)
        }
    }

    private suspend fun applyFavorite(scope: AccountScope, row: LibraryMutationEntity) {
        if (!row.desiredPresent) {
            favoritesDao.deleteByUrl(scope.serverId, scope.accountId, row.targetId)
            return
        }
        favoritesDao.upsert(
            FavoriteEntity(
                serverId = scope.serverId,
                accountId = scope.accountId,
                videoUrl = row.targetId,
                favoritedAtMillis = row.createdAtMillis,
                title = row.title,
                thumbnailUrl = row.thumbnailUrl,
                durationSeconds = row.durationSeconds,
                channelName = row.channelName,
                channelUrl = row.channelUrl,
                channelAvatarUrl = row.channelAvatarUrl,
                viewCount = row.viewCount,
            ),
        )
    }

    private suspend fun applyWatchLater(scope: AccountScope, row: LibraryMutationEntity) {
        if (!row.desiredPresent) {
            watchLaterDao.deleteByUrl(scope.serverId, scope.accountId, row.targetId)
            return
        }
        watchLaterDao.upsert(
            WatchLaterEntity(
                serverId = scope.serverId,
                accountId = scope.accountId,
                url = row.targetId,
                title = row.title,
                thumbnailUrl = row.thumbnailUrl,
                durationSeconds = row.durationSeconds,
                addedAtMillis = row.createdAtMillis,
                channelName = row.channelName,
                channelUrl = row.channelUrl,
                channelAvatarUrl = row.channelAvatarUrl,
                viewCount = row.viewCount,
            ),
        )
    }

    private suspend fun applySubscription(scope: AccountScope, row: LibraryMutationEntity) {
        if (!row.desiredPresent) {
            subscriptionDao.delete(scope.serverId, scope.accountId, row.targetId)
            return
        }
        subscriptionDao.upsert(
            SubscriptionEntity(
                serverId = scope.serverId,
                accountId = scope.accountId,
                channelUrl = row.targetId,
                name = row.title,
                avatarUrl = row.thumbnailUrl,
                subscribedAtMillis = row.createdAtMillis,
            ),
        )
    }

    private suspend fun applyPlaylistVideo(scope: AccountScope, row: LibraryMutationEntity) {
        val playlistId = row.parentId ?: return
        val cacheKey = PlaylistEntity.cacheKey(scope, playlistId)
        if (!row.desiredPresent) {
            val existed = playlistsDao.findVideoId(cacheKey, row.targetId) != null
            playlistsDao.deleteVideoFromPlaylist(cacheKey, row.targetId)
            if (existed) playlistsDao.adjustVideoCount(cacheKey, -1)
            return
        }
        if (!playlistsDao.containsPlaylist(cacheKey)) return
        val existingId = playlistsDao.findVideoId(cacheKey, row.targetId)
        val id = existingId ?: row.targetId
        playlistsDao.upsertVideos(
            listOf(
                PlaylistVideoEntity(
                    playlistCacheKey = cacheKey,
                    playlistId = playlistId,
                    id = id,
                    url = row.targetId,
                    title = row.title,
                    thumbnailUrl = row.thumbnailUrl,
                    durationSeconds = row.durationSeconds,
                    position = playlistsDao.nextVideoPosition(cacheKey),
                    channelName = row.channelName,
                    channelUrl = row.channelUrl,
                    channelAvatarUrl = row.channelAvatarUrl,
                    viewCount = row.viewCount,
                ),
            ),
        )
        if (existingId == null) playlistsDao.adjustVideoCount(cacheKey, 1)
    }
}
