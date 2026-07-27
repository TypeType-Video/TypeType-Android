package dev.typetype.android.data.library

import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.library.local.FavoriteEntity
import dev.typetype.android.data.library.local.HistoryEntity
import dev.typetype.android.data.library.local.PlaylistEntity
import dev.typetype.android.data.library.local.PlaylistVideoEntity
import dev.typetype.android.data.library.local.WatchLaterEntity
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.AddHistoryRequest
import dev.typetype.android.data.network.dto.AddPlaylistVideoRequest
import dev.typetype.android.data.network.dto.AddWatchLaterRequest
import dev.typetype.android.data.network.dto.CreatePlaylistRequest
import dev.typetype.android.data.network.dto.PlaylistReorderRequest
import dev.typetype.android.data.network.dto.SaveProgressRequest
import dev.typetype.android.data.network.dto.SubscriptionItemDto
import dev.typetype.android.data.network.requireSuccessfulResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class LibraryNetworkSource @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
) {

    internal suspend fun fetchHistory(
        scope: AccountScope,
        limit: Int = HISTORY_PAGE_SIZE,
        offset: Int = 0,
    ): HistoryPage = withContext(Dispatchers.IO) {
        val response = apiHolder.require(scope).history(limit = limit, offset = offset)
        response.requireSuccessfulResponse()
        val totalCount = response.headers()["X-Total-Count"]
            ?.toIntOrNull()
            ?.takeIf { it >= 0 }
            ?: error("History response is missing a valid X-Total-Count header")
        val body = response.body() ?: emptyList()
        val seen = HashSet<String>()
        val rows = body.asSequence()
            .filter { seen.add(it.url) }
            .map { dto -> dto.toHistoryEntity(scope) }
            .toList()
        HistoryPage(
            rows = rows,
            offset = offset,
            receivedCount = body.size,
            totalCount = totalCount,
        )
    }

    suspend fun fetchFavorites(scope: AccountScope): List<FavoriteEntity> = withContext(Dispatchers.IO) {
        val response = apiHolder.require(scope).favorites()
        response.requireSuccessfulResponse()
        (response.body() ?: emptyList()).map { dto ->
            FavoriteEntity(
                serverId = scope.serverId,
                accountId = scope.accountId,
                videoUrl = dto.videoUrl,
                favoritedAtMillis = dto.favoritedAt,
                title = dto.title,
                thumbnailUrl = dto.thumbnail,
                durationSeconds = dto.duration,
                channelName = dto.channelName,
                channelUrl = dto.channelUrl,
                channelAvatarUrl = dto.channelAvatar,
                viewCount = dto.viewCount,
            )
        }
    }

    suspend fun fetchWatchLater(scope: AccountScope): List<WatchLaterEntity> = withContext(Dispatchers.IO) {
        val response = apiHolder.require(scope).watchLater()
        response.requireSuccessfulResponse()
        (response.body() ?: emptyList()).map { dto ->
            WatchLaterEntity(
                serverId = scope.serverId,
                accountId = scope.accountId,
                url = dto.url,
                title = dto.title,
                thumbnailUrl = dto.thumbnail,
                durationSeconds = dto.duration,
                addedAtMillis = dto.addedAt,
                channelName = dto.channelName,
                channelUrl = dto.channelUrl,
                channelAvatarUrl = dto.channelAvatar,
                viewCount = dto.viewCount,
            )
        }
    }

    suspend fun fetchPlaylistSummaries(scope: AccountScope): List<PlaylistEntity> =
        withContext(Dispatchers.IO) {
            val response = apiHolder.require(scope).playlists()
            response.requireSuccessfulResponse()
            (response.body() ?: emptyList()).map { it.toPlaylistEntity(scope) }
        }

    suspend fun fetchPlaylist(
        scope: AccountScope,
        playlistId: String,
    ): Pair<PlaylistEntity, List<PlaylistVideoEntity>> = withContext(Dispatchers.IO) {
        val response = apiHolder.require(scope).playlist(playlistId)
        response.requireSuccessfulResponse()
        val dto = response.body() ?: error("Empty playlist response")
        check(dto.id == playlistId) { "Playlist response id does not match the request" }
        dto.toPlaylistEntity(scope) to dto.toVideoEntities(scope)
    }

    suspend fun postFavorite(scope: AccountScope, videoUrl: String) = withContext(Dispatchers.IO) {
        val response = apiHolder.require(scope).addFavorite(videoUrl)
        response.requireSuccessfulResponse()
    }

    suspend fun deleteFavorite(scope: AccountScope, videoUrl: String) = withContext(Dispatchers.IO) {
        val response = apiHolder.require(scope).removeFavorite(videoUrl)
        response.requireSuccessfulResponse()
    }

    suspend fun postWatchLater(
        scope: AccountScope,
        url: String,
        title: String,
        thumbnail: String,
        duration: Long,
        channelName: String,
        channelUrl: String,
        channelAvatar: String,
        viewCount: Long,
    ) =
        withContext(Dispatchers.IO) {
            val response = apiHolder.require(scope).addWatchLater(
                AddWatchLaterRequest(
                    url = url,
                    title = title,
                    thumbnail = thumbnail,
                    duration = duration,
                    channelName = channelName,
                    channelUrl = channelUrl,
                    channelAvatar = channelAvatar,
                    viewCount = viewCount,
                ),
            )
            response.requireSuccessfulResponse()
        }

    suspend fun deleteWatchLater(scope: AccountScope, url: String) = withContext(Dispatchers.IO) {
        val response = apiHolder.require(scope).removeWatchLater(url)
        response.requireSuccessfulResponse()
    }

    suspend fun postHistory(
        scope: AccountScope,
        videoUrl: String,
        title: String,
        thumbnail: String,
        duration: Long,
        channelName: String,
        channelUrl: String,
        channelAvatar: String,
    ): HistoryEntity? = withContext(Dispatchers.IO) {
        val response = apiHolder.require(scope).addHistory(
            AddHistoryRequest(
                url = videoUrl,
                title = title,
                thumbnail = thumbnail,
                duration = duration,
                channelName = channelName,
                channelUrl = channelUrl,
                channelAvatar = channelAvatar,
            ),
        )
        response.requireSuccessfulResponse()
        val dto = response.body() ?: error("Empty history response")
        dto.toPostedHistoryEntity(scope)
    }

    suspend fun deleteHistory(scope: AccountScope, id: String) = withContext(Dispatchers.IO) {
        val response = apiHolder.require(scope).removeHistory(id)
        if (!response.isSuccessful && response.code() != 404) response.requireSuccessfulResponse()
    }

    suspend fun getProgress(scope: AccountScope, videoUrl: String): Long? = withContext(Dispatchers.IO) {
        val response = apiHolder.require(scope).fetchProgress(videoUrl)
        when {
            response.isSuccessful -> response.body()?.position
            response.code() == 404 -> null
            else -> {
                response.requireSuccessfulResponse()
                null
            }
        }
    }

    suspend fun putProgress(scope: AccountScope, videoUrl: String, positionMillis: Long) = withContext(Dispatchers.IO) {
        val response = apiHolder.require(scope).saveProgress(
            videoUrl = videoUrl,
            body = SaveProgressRequest(position = positionMillis),
        )
        response.requireSuccessfulResponse()
    }

    suspend fun postCreatePlaylist(scope: AccountScope, name: String): PlaylistEntity = withContext(Dispatchers.IO) {
        val response = apiHolder.require(scope).createPlaylist(CreatePlaylistRequest(name = name))
        response.requireSuccessfulResponse()
        response.body()?.toPlaylistEntity(scope) ?: error("Empty playlist response")
    }

    suspend fun putPlaylist(
        scope: AccountScope,
        playlistId: String,
        name: String,
        description: String,
    ) = withContext(Dispatchers.IO) {
        val response = apiHolder.require(scope).updatePlaylist(
            playlistId = playlistId,
            body = CreatePlaylistRequest(name = name, description = description),
        )
        response.requireSuccessfulResponse()
    }

    suspend fun deletePlaylist(scope: AccountScope, playlistId: String) = withContext(Dispatchers.IO) {
        val response = apiHolder.require(scope).deletePlaylist(playlistId)
        response.requireSuccessfulResponse()
    }

    suspend fun putPlaylistOrder(
        scope: AccountScope,
        playlistId: String,
        orderedVideoUrls: List<String>,
    ) = withContext(Dispatchers.IO) {
        val response = apiHolder.require(scope).reorderPlaylist(
            playlistId = playlistId,
            body = PlaylistReorderRequest(orderedVideoUrls),
        )
        response.requireSuccessfulResponse()
    }

    suspend fun postAddVideoToPlaylist(
        scope: AccountScope,
        playlistId: String,
        url: String,
        title: String,
        thumbnail: String,
        duration: Long,
        channelName: String,
        channelUrl: String,
        channelAvatar: String,
        viewCount: Long,
    ) = withContext(Dispatchers.IO) {
        val response = apiHolder.require(scope).addVideoToPlaylist(
            playlistId = playlistId,
            body = AddPlaylistVideoRequest(
                url = url,
                title = title,
                thumbnail = thumbnail,
                duration = duration,
                channelName = channelName,
                channelUrl = channelUrl,
                channelAvatar = channelAvatar,
                viewCount = viewCount,
            ),
        )
        response.requireSuccessfulResponse()
    }

    suspend fun deleteVideoFromPlaylist(scope: AccountScope, playlistId: String, videoUrl: String) =
        withContext(Dispatchers.IO) {
            val response = apiHolder.require(scope).removeVideoFromPlaylist(
                playlistId = playlistId,
                videoUrl = videoUrl,
            )
            response.requireSuccessfulResponse()
        }

    suspend fun deleteAllHistory(scope: AccountScope) = withContext(Dispatchers.IO) {
        val response = apiHolder.require(scope).clearHistory()
        response.requireSuccessfulResponse()
    }

    suspend fun deleteAllSearchHistory(scope: AccountScope) = withContext(Dispatchers.IO) {
        val response = apiHolder.require(scope).clearSearchHistory()
        response.requireSuccessfulResponse()
    }

    suspend fun fetchSubscriptions(scope: AccountScope): List<dev.typetype.android.data.network.dto.SubscriptionItemDto> =
        withContext(Dispatchers.IO) {
            val response = apiHolder.require(scope).subscriptions()
            response.requireSuccessfulResponse()
            response.body() ?: emptyList()
        }

    suspend fun postSubscription(
        scope: AccountScope,
        channelUrl: String,
        name: String,
        avatarUrl: String,
    ) = withContext(Dispatchers.IO) {
        val response = apiHolder.require(scope).subscribe(
            SubscriptionItemDto(channelUrl = channelUrl, name = name, avatarUrl = avatarUrl),
        )
        response.requireSuccessfulResponse()
    }

    suspend fun deleteSubscription(scope: AccountScope, channelUrl: String) = withContext(Dispatchers.IO) {
        val response = apiHolder.require(scope).unsubscribe(channelUrl)
        response.requireSuccessfulResponse()
        }

    private companion object {
        const val HISTORY_PAGE_SIZE = 60
    }
}
