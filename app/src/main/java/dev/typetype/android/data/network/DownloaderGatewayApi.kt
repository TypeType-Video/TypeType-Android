package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.CreateDownloadJobRequest
import dev.typetype.android.data.network.dto.CreateDownloadJobResponse
import dev.typetype.android.data.network.dto.DownloadJobResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface DownloaderGatewayApi {
    @POST("downloader/jobs")
    suspend fun createJob(@Body body: CreateDownloadJobRequest): Response<CreateDownloadJobResponse>

    @GET("downloader/jobs/{id}")
    suspend fun job(@Path("id") id: String): Response<DownloadJobResponse>
}
