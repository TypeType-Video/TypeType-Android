package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.SessionDeviceRequest
import dev.typetype.android.data.network.dto.SessionPlaybackRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface TypeTypeActiveSessionApi {
    @POST("sessions/activity")
    suspend fun reportActivity(@Body request: SessionDeviceRequest): Response<Unit>

    @POST("sessions/playback/start")
    suspend fun reportPlaybackStart(@Body request: SessionPlaybackRequest): Response<Unit>

    @POST("sessions/playback/progress")
    suspend fun reportPlaybackProgress(@Body request: SessionPlaybackRequest): Response<Unit>

    @POST("sessions/playback/stop")
    suspend fun reportPlaybackStop(@Body request: SessionDeviceRequest): Response<Unit>
}
