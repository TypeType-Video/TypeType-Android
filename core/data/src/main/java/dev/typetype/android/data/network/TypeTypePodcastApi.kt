package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.ChannelPodcastPageDto
import dev.typetype.android.data.network.dto.PodcastEpisodesPageDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TypeTypePodcastApi {
    @GET("podcasts")
    suspend fun podcasts(
        @Query("url") channelUrl: String,
        @Query("nextpage") nextpage: String? = null,
    ): Response<ChannelPodcastPageDto>

    @GET("podcasts/episodes")
    suspend fun podcastEpisodes(
        @Query("url") podcastUrl: String,
        @Query("nextpage") nextpage: String? = null,
    ): Response<PodcastEpisodesPageDto>
}
