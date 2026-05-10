package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.AddHistoryRequest
import dev.typetype.android.data.network.dto.AddPlaylistVideoRequest
import dev.typetype.android.data.network.dto.AddWatchLaterRequest
import dev.typetype.android.data.network.dto.BlockChannelRequest
import dev.typetype.android.data.network.dto.BlockVideoRequest
import dev.typetype.android.data.network.dto.BlockedItemDto
import dev.typetype.android.data.network.dto.ChannelResponse
import dev.typetype.android.data.network.dto.CreatePlaylistRequest
import dev.typetype.android.data.network.dto.CommentsPageResponse
import dev.typetype.android.data.network.dto.FavoriteItemDto
import dev.typetype.android.data.network.dto.GuestResponse
import dev.typetype.android.data.network.dto.HealthResponse
import dev.typetype.android.data.network.dto.HistoryItemDto
import dev.typetype.android.data.network.dto.HomeRecommendationsResponse
import dev.typetype.android.data.network.dto.InstanceResponse
import dev.typetype.android.data.network.dto.LoginRequest
import dev.typetype.android.data.network.dto.PlaylistDto
import dev.typetype.android.data.network.dto.RefreshRequest
import dev.typetype.android.data.network.dto.RegisterRequest
import dev.typetype.android.data.network.dto.SaveProgressRequest
import dev.typetype.android.data.network.dto.SearchHistoryEntryRequest
import dev.typetype.android.data.network.dto.SearchResponse
import dev.typetype.android.data.network.dto.SessionResponse
import dev.typetype.android.data.network.dto.StreamResponse
import dev.typetype.android.data.network.dto.SubscriptionFeedResponse
import dev.typetype.android.data.network.dto.UserProfile
import dev.typetype.android.data.network.dto.UserSettingsDto
import dev.typetype.android.data.network.dto.VideoItem
import dev.typetype.android.data.network.dto.WatchLaterItemDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TypeTypeApi {

    @GET("health")
    suspend fun health(): Response<HealthResponse>

    @GET("instance")
    suspend fun instance(): Response<InstanceResponse>

    @POST("auth/guest")
    suspend fun guest(): Response<GuestResponse>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<SessionResponse>

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<SessionResponse>

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<SessionResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("auth/me")
    suspend fun me(): Response<UserProfile>

    @PUT("profile")
    suspend fun updateProfile(
        @Body body: dev.typetype.android.data.network.dto.ProfileUpdateRequest,
    ): Response<Unit>

    @PUT("profile/avatar/emoji")
    suspend fun setAvatarEmoji(
        @Body body: dev.typetype.android.data.network.dto.AvatarEmojiRequest,
    ): Response<Unit>

    @DELETE("profile/avatar")
    suspend fun clearAvatar(): Response<Unit>

    @GET("settings")
    suspend fun settings(): Response<UserSettingsDto>

    @PUT("settings")
    suspend fun updateSettings(@Body body: UserSettingsDto): Response<UserSettingsDto>

    @DELETE("history")
    suspend fun clearHistory(): Response<Unit>

    @GET("subscriptions")
    suspend fun subscriptions(): Response<List<dev.typetype.android.data.network.dto.SubscriptionItemDto>>

    @POST("subscriptions")
    suspend fun subscribe(
        @Body body: dev.typetype.android.data.network.dto.SubscriptionItemDto,
    ): Response<Unit>

    @DELETE("subscriptions")
    suspend fun unsubscribe(@Query("url") channelUrl: String): Response<Unit>

    @GET("recommendations/home")
    suspend fun homeRecommendations(
        @Query("service") service: Int = 0,
        @Query("limit") limit: Int = 30,
        @Query("intent") intent: String? = null,
        @Query("cursor") cursor: String? = null,
    ): Response<HomeRecommendationsResponse>

    @GET("trending")
    suspend fun trending(
        @Query("service") service: Int = 0,
    ): Response<List<VideoItem>>

    @GET("subscriptions/feed")
    suspend fun subscriptionsFeed(
        @Query("page") page: Int = 0,
        @Query("limit") limit: Int = 30,
    ): Response<SubscriptionFeedResponse>

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("service") service: Int = 0,
        @Query("nextpage") nextpage: String? = null,
    ): Response<SearchResponse>

    @GET("channel")
    suspend fun channel(
        @Query("url") url: String,
        @Query("nextpage") nextpage: String? = null,
    ): Response<ChannelResponse>

    @GET("history")
    suspend fun history(
        @Query("limit") limit: Int = 60,
        @Query("offset") offset: Int = 0,
    ): Response<List<HistoryItemDto>>

    @GET("favorites")
    suspend fun favorites(): Response<List<FavoriteItemDto>>

    @GET("watch-later")
    suspend fun watchLater(): Response<List<WatchLaterItemDto>>

    @GET("playlists")
    suspend fun playlists(): Response<List<PlaylistDto>>

    @POST("favorites/{videoUrl}")
    suspend fun addFavorite(@Path("videoUrl") videoUrl: String): Response<Unit>

    @DELETE("favorites/{videoUrl}")
    suspend fun removeFavorite(@Path("videoUrl") videoUrl: String): Response<Unit>

    @POST("watch-later")
    suspend fun addWatchLater(@Body body: AddWatchLaterRequest): Response<Unit>

    @DELETE("watch-later/{videoUrl}")
    suspend fun removeWatchLater(@Path("videoUrl") videoUrl: String): Response<Unit>

    @POST("history")
    suspend fun addHistory(@Body body: AddHistoryRequest): Response<Unit>

    @PUT("progress")
    suspend fun saveProgress(
        @Query("url") videoUrl: String,
        @Body body: SaveProgressRequest,
    ): Response<Unit>

    @GET("progress/{videoUrl}")
    suspend fun fetchProgress(
        @Path("videoUrl") videoUrl: String,
    ): Response<dev.typetype.android.data.network.dto.ProgressItemDto>

    @POST("playlists")
    suspend fun createPlaylist(@Body body: CreatePlaylistRequest): Response<PlaylistDto>

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

    @DELETE("blocked/videos/{videoUrl}")
    suspend fun unblockVideo(@Path("videoUrl") videoUrl: String): Response<Unit>

    @DELETE("blocked/channels/{channelUrl}")
    suspend fun unblockChannel(@Path("channelUrl") channelUrl: String): Response<Unit>

    @GET("suggestions")
    suspend fun searchSuggestions(
        @Query("query") query: String,
        @Query("service") service: Int = 0,
    ): Response<List<String>>

    @GET("search-history")
    suspend fun searchHistory(): Response<List<String>>

    @POST("search-history")
    suspend fun addSearchHistory(@Body body: SearchHistoryEntryRequest): Response<Unit>

    @DELETE("search-history")
    suspend fun removeSearchHistory(@Query("query") query: String): Response<Unit>

    @DELETE("search-history")
    suspend fun clearSearchHistory(): Response<Unit>

    @GET("streams")
    suspend fun streams(
        @Query("url") videoUrl: String,
    ): Response<StreamResponse>

    @GET("comments")
    suspend fun comments(
        @Query("url") videoUrl: String,
        @Query("nextpage") nextpage: String? = null,
    ): Response<CommentsPageResponse>

    @GET("comments/replies")
    suspend fun commentReplies(
        @Query("url") videoUrl: String,
        @Query("repliesPage") repliesPage: String,
    ): Response<CommentsPageResponse>
}
