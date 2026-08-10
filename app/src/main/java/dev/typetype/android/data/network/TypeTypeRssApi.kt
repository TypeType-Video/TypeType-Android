package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.RssFeedEnabledRequestDto
import dev.typetype.android.data.network.dto.RssFeedItemDto
import dev.typetype.android.data.network.dto.RssFeedRequestDto
import dev.typetype.android.data.network.dto.RssFeedSecretItemDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface TypeTypeRssApi {
    @GET("rss/feeds")
    suspend fun rssFeeds(): Response<List<RssFeedItemDto>>

    @POST("rss/feeds")
    suspend fun createRssFeed(@Body request: RssFeedRequestDto): Response<RssFeedSecretItemDto>

    @PUT("rss/feeds/{id}")
    suspend fun updateRssFeed(
        @Path("id") id: String,
        @Body request: RssFeedRequestDto,
    ): Response<RssFeedItemDto>

    @PUT("rss/feeds/{id}/enabled")
    suspend fun setRssFeedEnabled(
        @Path("id") id: String,
        @Body request: RssFeedEnabledRequestDto,
    ): Response<RssFeedItemDto>

    @POST("rss/feeds/{id}/regenerate")
    suspend fun regenerateRssFeed(@Path("id") id: String): Response<RssFeedSecretItemDto>

    @DELETE("rss/feeds/{id}")
    suspend fun deleteRssFeed(@Path("id") id: String): Response<Unit>
}
