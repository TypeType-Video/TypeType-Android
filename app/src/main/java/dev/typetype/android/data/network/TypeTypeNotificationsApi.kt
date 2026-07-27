package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.MarkNotificationsReadResponseDto
import dev.typetype.android.data.network.dto.NotificationsResponseDto
import dev.typetype.android.data.network.dto.UnreadNotificationsCountDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
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
}
