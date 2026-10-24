package dev.typetype.android.data.library

import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.library.local.FavoriteEntity
import dev.typetype.android.data.library.local.FavoritesDao
import dev.typetype.android.data.library.local.PlaylistEntity
import dev.typetype.android.data.library.local.PlaylistVideoEntity
import dev.typetype.android.data.library.local.PlaylistsDao
import dev.typetype.android.data.library.local.WatchLaterDao
import dev.typetype.android.data.library.local.WatchLaterEntity
import dev.typetype.android.data.library.sync.LibraryMutationKind
import dev.typetype.android.data.library.sync.LibraryMutationRequest
import dev.typetype.android.data.library.sync.LibraryMutationWriter
import dev.typetype.android.domain.library.LibraryCollection
import javax.inject.Inject

class LibraryOptimisticMutations @Inject constructor(
    private val activeAccountScope: ActiveAccountScope,
    private val favoritesDao: FavoritesDao,
    private val watchLaterDao: WatchLaterDao,
    private val playlistsDao: PlaylistsDao,
    private val writer: LibraryMutationWriter,
) {
    suspend fun favorite(video: MutationVideo, desiredPresent: Boolean) {
        val scope = activeAccountScope.require()
        writer.enqueue(
            scope,
            video.request(LibraryMutationKind.Favorite, desiredPresent = desiredPresent),
        ) {
            if (desiredPresent) {
                favoritesDao.upsert(
                    FavoriteEntity(
                        serverId = scope.serverId,
                        accountId = scope.accountId,
                        videoUrl = video.url,
                        favoritedAtMillis = System.currentTimeMillis(),
                        title = video.title,
                        thumbnailUrl = video.thumbnailUrl,
                        durationSeconds = video.durationSeconds,
                        channelName = video.channelName,
                        channelUrl = video.channelUrl,
                        channelAvatarUrl = video.channelAvatarUrl,
                        viewCount = video.viewCount,
                    ),
                )
            } else {
                favoritesDao.deleteByUrl(scope.serverId, scope.accountId, video.url)
            }
        }
    }

    suspend fun watchLater(video: MutationVideo, desiredPresent: Boolean) {
        val scope = activeAccountScope.require()
        writer.enqueue(
            scope,
            video.request(LibraryMutationKind.WatchLater, desiredPresent = desiredPresent),
        ) {
            if (desiredPresent) {
                watchLaterDao.upsert(
                    WatchLaterEntity(
                        serverId = scope.serverId,
                        accountId = scope.accountId,
                        url = video.url,
                        title = video.title,
                        thumbnailUrl = video.thumbnailUrl,
                        durationSeconds = video.durationSeconds,
                        addedAtMillis = System.currentTimeMillis(),
                        channelName = video.channelName,
                        channelUrl = video.channelUrl,
                        channelAvatarUrl = video.channelAvatarUrl,
                        viewCount = video.viewCount,
                    ),
                )
            } else {
                watchLaterDao.deleteByUrl(scope.serverId, scope.accountId, video.url)
            }
        }
    }

    suspend fun playlistVideo(playlistId: String, video: MutationVideo, desiredPresent: Boolean) {
        val scope = activeAccountScope.require()
        val cacheKey = PlaylistEntity.cacheKey(scope, playlistId)
        check(playlistsDao.containsPlaylist(cacheKey)) { "Playlist not found" }
        writer.enqueue(
            scope,
            video.request(
                LibraryMutationKind.PlaylistVideo,
                parentId = playlistId,
                desiredPresent = desiredPresent,
            ),
        ) {
            if (desiredPresent) {
                val existingId = playlistsDao.findVideoId(cacheKey, video.url)
                val id = existingId ?: video.url
                playlistsDao.upsertVideos(
                    listOf(
                        PlaylistVideoEntity(
                            playlistCacheKey = cacheKey,
                            playlistId = playlistId,
                            id = id,
                            url = video.url,
                            title = video.title,
                            thumbnailUrl = video.thumbnailUrl,
                            durationSeconds = video.durationSeconds,
                            position = playlistsDao.nextVideoPosition(cacheKey),
                            channelName = video.channelName,
                            channelUrl = video.channelUrl,
                            channelAvatarUrl = video.channelAvatarUrl,
                            viewCount = video.viewCount,
                        ),
                    ),
                )
                if (existingId == null) playlistsDao.adjustVideoCount(cacheKey, 1)
            } else {
                val existed = playlistsDao.findVideoId(cacheKey, video.url) != null
                playlistsDao.deleteVideoFromPlaylist(cacheKey, video.url)
                if (existed) playlistsDao.adjustVideoCount(cacheKey, -1)
            }
        }
    }

    suspend fun recordCreatedPlaylist(playlist: PlaylistEntity) = playlistsDao.upsertPlaylist(playlist)

    suspend fun recordRenamedPlaylist(playlistId: String, name: String) {
        val scope = activeAccountScope.require()
        playlistsDao.renamePlaylist(PlaylistEntity.cacheKey(scope, playlistId), name)
    }

    suspend fun recordDeletedPlaylist(playlistId: String) {
        val scope = activeAccountScope.require()
        playlistsDao.deletePlaylist(PlaylistEntity.cacheKey(scope, playlistId))
    }

    suspend fun retry(collection: LibraryCollection): Boolean {
        val scope = activeAccountScope.require()
        return writer.retry(scope, collection)
    }

    suspend fun resume(): Boolean = writer.resume(activeAccountScope.require())
}

data class MutationVideo(
    val url: String,
    val title: String = "",
    val thumbnailUrl: String = "",
    val durationSeconds: Long = 0L,
    val channelName: String = "",
    val channelUrl: String = "",
    val channelAvatarUrl: String = "",
    val viewCount: Long = 0L,
)

private fun MutationVideo.request(
    kind: LibraryMutationKind,
    parentId: String? = null,
    desiredPresent: Boolean,
) = LibraryMutationRequest(
    kind = kind,
    targetId = url,
    parentId = parentId,
    desiredPresent = desiredPresent,
    title = title,
    thumbnailUrl = thumbnailUrl,
    durationSeconds = durationSeconds,
    channelName = channelName,
    channelUrl = channelUrl,
    channelAvatarUrl = channelAvatarUrl,
    viewCount = viewCount,
)
