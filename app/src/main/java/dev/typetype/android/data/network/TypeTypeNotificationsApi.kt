package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.MarkNotificationsReadResponseDto
import dev.typetype.android.data.network.dto.NotificationsResponseDto
import dev.typetype.android.data.network.dto.ChannelNotificationPreferenceDto
import dev.typetype.android.data.network.dto.ChannelNotificationPreferenceRequestDto
import dev.typetype.android.data.network.dto.PushDeviceRegistrationRequestDto
import dev.typetype.android.data.network.dto.PushDeviceRegistrationResponseDto
import dev.typetype.android.data.network.dto.UnreadNotificationsCountDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TypeTypeNotificationsApi {
    @GET("notifications/unread-count")
    suspend fun unreadNotificationsCount(): Response<UnreadNotificationsCountDto>

    @GET("notifications")
    suspend fun notifications(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): Response<NotificationsResponseDto>

    @POST("notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<MarkNotificationsReadResponseDto>

    @POST("notifications/push/devices")
    suspend fun registerPushDevice(
        @Body request: PushDeviceRegistrationRequestDto,
    ): Response<PushDeviceRegistrationResponseDto>

    @GET("notifications/push/devices")
    suspend fun pushDevices(): Response<List<PushDeviceRegistrationResponseDto>>

    @DELETE("notifications/push/devices/{deviceId}")
    suspend fun deletePushDevice(
        @Path("deviceId") deviceId: String,
    ): Response<Unit>

    @GET("notifications/channel-preferences")
    suspend fun channelNotificationPreferences(): Response<List<ChannelNotificationPreferenceDto>>

    @PUT("notifications/channel-preferences")
    suspend fun setChannelNotificationPreference(
        @Body request: ChannelNotificationPreferenceRequestDto,
    ): Response<ChannelNotificationPreferenceDto>
}
