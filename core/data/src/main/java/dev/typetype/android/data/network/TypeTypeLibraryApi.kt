package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.AddHistoryRequest
import dev.typetype.android.data.network.dto.AddPlaylistVideoRequest
import dev.typetype.android.data.network.dto.AddWatchLaterRequest
import dev.typetype.android.data.network.dto.BlockChannelRequest
import dev.typetype.android.data.network.dto.BlockedKeywordDto
import dev.typetype.android.data.network.dto.BlockedKeywordRequest
import dev.typetype.android.data.network.dto.BlockVideoRequest
import dev.typetype.android.data.network.dto.BlockedItemDto
import dev.typetype.android.data.network.dto.CreatePlaylistRequest
import dev.typetype.android.data.network.dto.FavoriteItemDto
import dev.typetype.android.data.network.dto.HistoryItemDto
import dev.typetype.android.data.network.dto.PlaylistDto
import dev.typetype.android.data.network.dto.PlaylistReorderRequest
import dev.typetype.android.data.network.dto.ProgressItemDto
import dev.typetype.android.data.network.dto.SaveProgressRequest
import dev.typetype.android.data.network.dto.SavePublicPlaylistRequest
import dev.typetype.android.data.network.dto.SavedPublicPlaylistDto
import dev.typetype.android.data.network.dto.WatchLaterItemDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TypeTypeLibraryApi {
    @GET("history")
    suspend fun history(
        @Query("q") search: String? = null,
        @Query("from") fromMillis: Long? = null,
        @Query("to") toMillis: Long? = null,
        @Query("limit") limit: Int = 60,
        @Query("offset") offset: Int = 0,
    ): Response<List<HistoryItemDto>>

    @GET("favorites")
    suspend fun favorites(): Response<List<FavoriteItemDto>>

    @GET("watch-later")
    suspend fun watchLater(): Response<List<WatchLaterItemDto>>

    @GET("playlists")
    suspend fun playlists(): Response<List<PlaylistDto>>

    @GET("playlists/{id}")
    suspend fun playlist(@Path("id") playlistId: String): Response<PlaylistDto>

    @GET("saved-playlists")
    suspend fun savedPublicPlaylists(): Response<List<SavedPublicPlaylistDto>>

    @POST("saved-playlists")
    suspend fun savePublicPlaylist(
        @Body body: SavePublicPlaylistRequest,
    ): Response<SavedPublicPlaylistDto>

    @DELETE("saved-playlists/{id}")
    suspend fun removeSavedPublicPlaylist(@Path("id") id: String): Response<Unit>

    @POST("favorites/{videoUrl}")
    suspend fun addFavorite(@Path("videoUrl") videoUrl: String): Response<Unit>

    @DELETE("favorites/{videoUrl}")
    suspend fun removeFavorite(@Path("videoUrl") videoUrl: String): Response<Unit>

    @POST("watch-later")
    suspend fun addWatchLater(@Body body: AddWatchLaterRequest): Response<Unit>

    @DELETE("watch-later/{videoUrl}")
    suspend fun removeWatchLater(@Path("videoUrl") videoUrl: String): Response<Unit>

    @POST("history")
    suspend fun addHistory(@Body body: AddHistoryRequest): Response<HistoryItemDto>

    @DELETE("history/{id}")
    suspend fun removeHistory(@Path("id") id: String): Response<Unit>

    @PUT("progress")
    suspend fun saveProgress(
        @Query("url") videoUrl: String,
        @Body body: SaveProgressRequest,
    ): Response<Unit>

    @GET("progress/{videoUrl}")
    suspend fun fetchProgress(@Path("videoUrl") videoUrl: String): Response<ProgressItemDto>

    @POST("playlists")
    suspend fun createPlaylist(@Body body: CreatePlaylistRequest): Response<PlaylistDto>

    @PUT("playlists/{id}")
    suspend fun updatePlaylist(
        @Path("id") playlistId: String,
        @Body body: CreatePlaylistRequest,
    ): Response<Unit>

    @DELETE("playlists/{id}")
    suspend fun deletePlaylist(@Path("id") playlistId: String): Response<Unit>

    @PUT("playlists/{id}/reorder")
    suspend fun reorderPlaylist(
        @Path("id") playlistId: String,
        @Body body: PlaylistReorderRequest,
    ): Response<Unit>

    @POST("playlists/{id}/videos")
    suspend fun addVideoToPlaylist(
        @Path("id") playlistId: String,
        @Body body: AddPlaylistVideoRequest,
    ): Response<Unit>

    @DELETE("playlists/{id}/videos/{videoUrl}")
    suspend fun removeVideoFromPlaylist(
        @Path("id") playlistId: String,
        @Path("videoUrl") videoUrl: String,
    ): Response<Unit>

    @POST("blocked/videos")
    suspend fun blockVideo(@Body body: BlockVideoRequest): Response<Unit>

    @POST("blocked/channels")
    suspend fun blockChannel(@Body body: BlockChannelRequest): Response<Unit>

    @GET("blocked/videos")
    suspend fun blockedVideos(): Response<List<BlockedItemDto>>

    @GET("blocked/channels")
    suspend fun blockedChannels(): Response<List<BlockedItemDto>>

    @GET("blocked/keywords")
    suspend fun blockedKeywords(): Response<List<BlockedKeywordDto>>

    @POST("blocked/keywords")
    suspend fun blockKeyword(@Body body: BlockedKeywordRequest): Response<BlockedKeywordDto>

    @DELETE("blocked/videos/{videoUrl}")
    suspend fun unblockVideo(@Path("videoUrl") videoUrl: String): Response<Unit>

    @DELETE("blocked/channels/{channelUrl}")
    suspend fun unblockChannel(@Path("channelUrl") channelUrl: String): Response<Unit>

    @DELETE("blocked/keywords/{keyword}")
    suspend fun unblockKeyword(@Path("keyword") keyword: String): Response<Unit>
}
