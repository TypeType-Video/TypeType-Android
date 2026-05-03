package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.ChannelResponse
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
import dev.typetype.android.data.network.dto.SearchResponse
import dev.typetype.android.data.network.dto.SessionResponse
import dev.typetype.android.data.network.dto.StreamResponse
import dev.typetype.android.data.network.dto.SubscriptionFeedResponse
import dev.typetype.android.data.network.dto.UserProfile
import dev.typetype.android.data.network.dto.VideoItem
import dev.typetype.android.data.network.dto.WatchLaterItemDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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

    @GET("streams")
    suspend fun streams(
        @Query("url") videoUrl: String,
    ): Response<StreamResponse>

    @GET("comments")
    suspend fun comments(
        @Query("url") videoUrl: String,
        @Query("nextpage") nextpage: String? = null,
    ): Response<CommentsPageResponse>
}
