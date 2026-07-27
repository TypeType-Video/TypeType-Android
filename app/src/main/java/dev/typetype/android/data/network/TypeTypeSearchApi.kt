package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.ChannelResponse
import dev.typetype.android.data.network.dto.SearchHistoryEntryRequest
import dev.typetype.android.data.network.dto.SearchFiltersResponse
import dev.typetype.android.data.network.dto.PublicPlaylistResponseDto
import dev.typetype.android.data.network.dto.SearchResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface TypeTypeSearchApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("service") service: Int = 0,
        @Query("nextpage") nextpage: String? = null,
        @Query("contentFilter") contentFilter: String? = null,
        @Query("sortFilter") sortFilter: String? = null,
    ): Response<SearchResponse>

    @GET("search/filters")
    suspend fun searchFilters(
        @Query("service") service: Int = 0,
    ): Response<SearchFiltersResponse>

    @GET("playlist")
    suspend fun publicPlaylist(
        @Query("url") url: String,
        @Query("nextpage") nextpage: String? = null,
    ): Response<PublicPlaylistResponseDto>

    @GET("channel")
    suspend fun channel(
        @Query("url") url: String,
        @Query("nextpage") nextpage: String? = null,
    ): Response<ChannelResponse>

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
}
