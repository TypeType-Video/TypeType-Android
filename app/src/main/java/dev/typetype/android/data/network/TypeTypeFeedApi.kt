package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.HomeRecommendationsResponse
import dev.typetype.android.data.network.dto.SubscriptionFeedResponse
import dev.typetype.android.data.network.dto.SubscriptionItemDto
import dev.typetype.android.data.network.dto.VideoItem
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface TypeTypeFeedApi {
    @GET("subscriptions")
    suspend fun subscriptions(): Response<List<SubscriptionItemDto>>

    @POST("subscriptions")
    suspend fun subscribe(@Body body: SubscriptionItemDto): Response<Unit>

    @DELETE("subscriptions")
    suspend fun unsubscribe(@Query("url") channelUrl: String): Response<Unit>

    @GET("recommendations/home")
    suspend fun homeRecommendations(
        @Query("service") service: Int = 0,
        @Query("limit") limit: Int = 30,
        @Query("intent") intent: String? = null,
        @Query("cursor") cursor: String? = null,
    ): Response<HomeRecommendationsResponse>

    @GET("recommendations/shorts")
    suspend fun shortsRecommendations(
        @Query("service") service: Int = 0,
        @Query("limit") limit: Int = 30,
        @Query("intent") intent: String = "auto",
        @Query("cursor") cursor: String? = null,
    ): Response<HomeRecommendationsResponse>

    @GET("trending")
    suspend fun trending(@Query("service") service: Int = 0): Response<List<VideoItem>>

    @GET("subscriptions/feed")
    suspend fun subscriptionsFeed(
        @Query("limit") limit: Int = 30,
        @Query("cursor") cursor: String? = null,
    ): Response<SubscriptionFeedResponse>

    @GET("subscriptions/shorts")
    suspend fun subscriptionShorts(
        @Query("page") page: Int = 0,
        @Query("limit") limit: Int = 30,
        @Query("service") service: Int = 0,
        @Query("blended") blended: Boolean = true,
    ): Response<SubscriptionFeedResponse>
}
