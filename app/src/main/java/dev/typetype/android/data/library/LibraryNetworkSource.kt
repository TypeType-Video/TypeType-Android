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
        // Server returns rows sorted by watchedAt DESC. Multiple POST /history
        // calls for the same video produce duplicates server-side, so we dedup
        // by URL keeping the first occurrence (= most recent watch). Same
        // pattern as the TypeType web client (apps/web/src/hooks/use-history.ts).
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
    ) = withContext(Dispatchers.IO) {
        val response = apiHolder.require().addHistory(
            AddHistoryRequest(
                url = videoUrl,
                title = title,
                thumbnail = thumbnail,
                duration = duration,
                channelName = channelName,
                channelUrl = channelUrl,
            ),
        )
        if (!response.isSuccessful) error("Add history failed (HTTP ${response.code()})")
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
    ) = withContext(Dispatchers.IO) {
        val response = apiHolder.require().addVideoToPlaylist(
            playlistId = playlistId,
            body = AddPlaylistVideoRequest(
                url = url,
                title = title,
                thumbnail = thumbnail,
                duration = duration,
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
}
