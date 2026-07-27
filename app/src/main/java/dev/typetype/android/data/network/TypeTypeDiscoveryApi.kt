package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.HealthResponse
import dev.typetype.android.data.network.dto.InstanceResponse
import retrofit2.Response
import retrofit2.http.GET

interface TypeTypeDiscoveryApi {
    @GET("health")
    suspend fun health(): Response<HealthResponse>

    @GET("instance")
    suspend fun instance(): Response<InstanceResponse>
}
