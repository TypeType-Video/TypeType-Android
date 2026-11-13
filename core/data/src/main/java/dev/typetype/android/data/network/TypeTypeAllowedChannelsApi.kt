package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.AllowedChannelDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface TypeTypeAllowedChannelsApi {
    @GET("allowed/channels")
    suspend fun allowedChannels(): Response<List<AllowedChannelDto>>

    @POST("allowed/channels")
    suspend fun allowChannel(@Body body: AllowedChannelDto): Response<AllowedChannelDto>
}
