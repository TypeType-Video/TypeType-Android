package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.DeviceLookupResponse
import dev.typetype.android.data.network.dto.DeviceUserCodeRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface TypeTypeDeviceLinkApi {
    @GET("auth/device/lookup")
    suspend fun lookupDeviceLink(@Query("code") userCode: String): Response<DeviceLookupResponse>

    @POST("auth/device/approve")
    suspend fun approveDeviceLink(@Body body: DeviceUserCodeRequest): Response<Unit>

    @POST("auth/device/deny")
    suspend fun denyDeviceLink(@Body body: DeviceUserCodeRequest): Response<Unit>
}
