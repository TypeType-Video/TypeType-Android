package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.ComponentVersionDto
import retrofit2.Response
import retrofit2.http.GET

interface TypeTypeVersionApi {
    @GET("version/web")
    suspend fun frontendVersion(): Response<ComponentVersionDto>

    @GET("version/server")
    suspend fun serverVersion(): Response<ComponentVersionDto>

    @GET("version/token")
    suspend fun tokenVersion(): Response<ComponentVersionDto>

    @GET("version/downloader")
    suspend fun downloaderVersion(): Response<ComponentVersionDto>
}
