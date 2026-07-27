package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.CreateDownloadJobRequest
import dev.typetype.android.data.network.dto.CreateDownloadJobResponse
import dev.typetype.android.data.network.dto.DownloadJobResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface DownloaderGatewayApi {
    @POST("downloader/jobs")
    suspend fun createJob(
        @Body body: CreateDownloadJobRequest,
        @Header("Authorization") authorization: String? = null,
    ): Response<CreateDownloadJobResponse>

    @GET("downloader/jobs/{id}")
    suspend fun job(
        @Path("id") id: String,
        @Header("Authorization") authorization: String? = null,
    ): Response<DownloadJobResponse>

    @POST("downloader/jobs/{id}/cancel")
    suspend fun cancelJob(
        @Path("id") id: String,
        @Header("Authorization") authorization: String? = null,
    ): Response<DownloadJobResponse>

    @DELETE("downloader/jobs/{id}")
    suspend fun deleteJob(
        @Path("id") id: String,
        @Header("Authorization") authorization: String? = null,
    ): Response<Unit>
}
