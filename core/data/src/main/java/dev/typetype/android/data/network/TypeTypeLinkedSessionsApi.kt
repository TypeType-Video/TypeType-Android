package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.BiliBiliQrLoginResponse
import dev.typetype.android.data.network.dto.BiliBiliQrPollRequest
import dev.typetype.android.data.network.dto.BiliBiliQrPollResponse
import dev.typetype.android.data.network.dto.BiliBiliSessionStatusResponse
import dev.typetype.android.data.network.dto.YoutubeDevicePollResponse
import dev.typetype.android.data.network.dto.YoutubeDeviceStartResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST

interface TypeTypeLinkedSessionsApi {
    @POST("youtube-session/device/start")
    suspend fun startYoutubeDeviceLogin(): Response<YoutubeDeviceStartResponse>

    @GET("youtube-session/device/status")
    suspend fun pollYoutubeDeviceLogin(): Response<YoutubeDevicePollResponse>

    @POST("youtube-session/device/cancel")
    suspend fun cancelYoutubeDeviceLogin(): Response<Unit>

    @GET("bilibili-session/status")
    suspend fun biliBiliSessionStatus(): Response<BiliBiliSessionStatusResponse>

    @POST("bilibili-session/qr")
    suspend fun startBiliBiliQrLogin(): Response<BiliBiliQrLoginResponse>

    @POST("bilibili-session/qr/poll")
    suspend fun pollBiliBiliQrLogin(@Body body: BiliBiliQrPollRequest): Response<BiliBiliQrPollResponse>

    @DELETE("bilibili-session")
    suspend fun disconnectBiliBiliSession(): Response<Unit>
}
