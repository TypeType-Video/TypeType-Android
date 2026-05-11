package dev.typetype.android.data.library

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
import dev.typetype.android.data.network.dto.SaveProgressRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class LibraryNetworkSource @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
) {

    suspend fun fetchHistory(): List<HistoryEntity> = withContext(Dispatchers.IO) {
        val response = apiHolder.require().history()
        if (!response.isSuccessful) error("History failed (HTTP ${response.code()})")
        val seen = HashSet<String>()
        (response.body() ?: emptyList()).asSequence()
            .filter { seen.add(it.url) }
            .map { dto ->
                HistoryEntity(
                    id = dto.id,
                    url = dto.url,
                    title = dto.title,
                    thumbnailUrl = dto.thumbnail,
                    channelName = dto.channelName,
                    channelUrl = dto.channelUrl,
                    channelAvatarUrl = dto.channelAvatar,
                    durationSeconds = dto.duration,
                    progressSeconds = dto.progress,
                    watchedAtMillis = dto.watchedAt,
                )
            }
            .toList()
    }

    suspend fun fetchFavorites(): List<FavoriteEntity> = withContext(Dispatchers.IO) {
        val response = apiHolder.require().favorites()
        if (!response.isSuccessful) error("Favorites failed (HTTP ${response.code()})")
        (response.body() ?: emptyList()).map { dto ->
            FavoriteEntity(videoUrl = dto.videoUrl, favoritedAtMillis = dto.favoritedAt)
        }
    }

    suspend fun fetchWatchLater(): List<WatchLaterEntity> = withContext(Dispatchers.IO) {
        val response = apiHolder.require().watchLater()
        if (!response.isSuccessful) error("Watch later failed (HTTP ${response.code()})")
        (response.body() ?: emptyList()).map { dto ->
            WatchLaterEntity(
                url = dto.url,
                title = dto.title,
                thumbnailUrl = dto.thumbnail,
                durationSeconds = dto.duration,
                addedAtMillis = dto.addedAt,
            )
        }
    }

    suspend fun fetchPlaylists(): Pair<List<PlaylistEntity>, List<PlaylistVideoEntity>> =
        withContext(Dispatchers.IO) {
            val response = apiHolder.require().playlists()
            if (!response.isSuccessful) error("Playlists failed (HTTP ${response.code()})")
            val dtos = response.body() ?: emptyList()
            val playlists = dtos.map { dto ->
                PlaylistEntity(
                    id = dto.id,
                    name = dto.name,
                    description = dto.description,
                    createdAtMillis = dto.createdAt,
                )
            }
            val videos = dtos.flatMap { dto ->
                dto.videos.map { v ->
                    PlaylistVideoEntity(
                        playlistId = dto.id,
                        id = v.id,
                        url = v.url,
                        title = v.title,
                        thumbnailUrl = v.thumbnail,
                        durationSeconds = v.duration,
                        position = v.position,
                        channelName = v.channelName,
                        channelUrl = v.channelUrl,
                        channelAvatarUrl = v.channelAvatar,
                        viewCount = v.viewCount,
                    )
                }
            }
            playlists to videos
        }

    suspend fun postFavorite(videoUrl: String) = withContext(Dispatchers.IO) {
        val response = apiHolder.require().addFavorite(videoUrl)
        if (!response.isSuccessful) error("Add favorite failed (HTTP ${response.code()})")
    }

    suspend fun deleteFavorite(videoUrl: String) = withContext(Dispatchers.IO) {
        val response = apiHolder.require().removeFavorite(videoUrl)
        if (!response.isSuccessful) error("Remove favorite failed (HTTP ${response.code()})")
    }

    suspend fun postWatchLater(url: String, title: String, thumbnail: String, duration: Long) =
        withContext(Dispatchers.IO) {
            val response = apiHolder.require().addWatchLater(
                AddWatchLaterRequest(url = url, title = title, thumbnail = thumbnail, duration = duration),
            )
            if (!response.isSuccessful) error("Add watch later failed (HTTP ${response.code()})")
        }

    suspend fun deleteWatchLater(url: String) = withContext(Dispatchers.IO) {
        val response = apiHolder.require().removeWatchLater(url)
        if (!response.isSuccessful) error("Remove watch later failed (HTTP ${response.code()})")
    }

    suspend fun postHistory(
        videoUrl: String,
        title: String,
        thumbnail: String,
        duration: Long,
        channelName: String,
        channelUrl: String,
        channelAvatar: String,
    ) = withContext(Dispatchers.IO) {
        val response = apiHolder.require().addHistory(
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
        if (!response.isSuccessful) error("Add history failed (HTTP ${response.code()})")
    }

    suspend fun getProgress(videoUrl: String): Long? = withContext(Dispatchers.IO) {
        val response = apiHolder.require().fetchProgress(videoUrl)
        when {
            response.isSuccessful -> response.body()?.position
            response.code() == 404 -> null
            else -> error("Fetch progress failed (HTTP ${response.code()})")
        }
    }

    suspend fun putProgress(videoUrl: String, positionMillis: Long) = withContext(Dispatchers.IO) {
        val response = apiHolder.require().saveProgress(
            videoUrl = videoUrl,
            body = SaveProgressRequest(position = positionMillis),
        )
        if (!response.isSuccessful) error("Save progress failed (HTTP ${response.code()})")
    }

    suspend fun postCreatePlaylist(name: String): String = withContext(Dispatchers.IO) {
        val response = apiHolder.require().createPlaylist(CreatePlaylistRequest(name = name))
        if (!response.isSuccessful) error("Create playlist failed (HTTP ${response.code()})")
        response.body()?.id ?: error("Empty playlist id in response")
    }

    suspend fun postAddVideoToPlaylist(
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
        val response = apiHolder.require().addVideoToPlaylist(
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
        if (!response.isSuccessful) error("Add to playlist failed (HTTP ${response.code()})")
    }

    suspend fun deleteVideoFromPlaylist(playlistId: String, videoUrl: String) =
        withContext(Dispatchers.IO) {
            val response = apiHolder.require().removeVideoFromPlaylist(
                playlistId = playlistId,
                videoUrl = videoUrl,
            )
            if (!response.isSuccessful) {
                error("Remove from playlist failed (HTTP ${response.code()})")
            }
        }

    suspend fun deleteAllHistory() = withContext(Dispatchers.IO) {
        val response = apiHolder.require().clearHistory()
        if (!response.isSuccessful) error("Clear history failed (HTTP ${response.code()})")
    }

    suspend fun deleteAllSearchHistory() = withContext(Dispatchers.IO) {
        val response = apiHolder.require().clearSearchHistory()
        if (!response.isSuccessful) error("Clear search history failed (HTTP ${response.code()})")
    }

    suspend fun fetchSubscriptions(): List<dev.typetype.android.data.network.dto.SubscriptionItemDto> =
        withContext(Dispatchers.IO) {
            val response = apiHolder.require().subscriptions()
            if (!response.isSuccessful) error("Subscriptions failed (HTTP ${response.code()})")
            response.body() ?: emptyList()
        }

    suspend fun deleteSubscription(channelUrl: String) = withContext(Dispatchers.IO) {
        val response = apiHolder.require().unsubscribe(channelUrl)
        if (!response.isSuccessful) error("Unsubscribe failed (HTTP ${response.code()})")
    }
}
