package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.AvatarEmojiRequest
import dev.typetype.android.data.network.dto.ProfileUpdateRequest
import dev.typetype.android.data.network.dto.UserSettingsDto
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT

interface TypeTypeProfileApi {
    @PUT("profile")
    suspend fun updateProfile(@Body body: ProfileUpdateRequest): Response<Unit>

    @PUT("profile/avatar/emoji")
    suspend fun setAvatarEmoji(@Body body: AvatarEmojiRequest): Response<Unit>

    @DELETE("profile/avatar")
    suspend fun clearAvatar(): Response<Unit>

    @GET("settings")
    suspend fun settings(): Response<UserSettingsDto>

    @PUT("settings")
    suspend fun updateSettings(@Body body: JsonObject): Response<UserSettingsDto>

    @DELETE("history")
    suspend fun clearHistory(): Response<Unit>
}
