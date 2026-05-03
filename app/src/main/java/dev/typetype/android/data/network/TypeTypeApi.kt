package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.GuestResponse
import dev.typetype.android.data.network.dto.LoginRequest
import dev.typetype.android.data.network.dto.RefreshRequest
import dev.typetype.android.data.network.dto.RegisterRequest
import dev.typetype.android.data.network.dto.SessionResponse
import dev.typetype.android.data.network.dto.UserProfile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface TypeTypeApi {

    @POST("auth/guest")
    suspend fun guest(): Response<GuestResponse>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<SessionResponse>

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<SessionResponse>

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<SessionResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("auth/me")
    suspend fun me(): Response<UserProfile>
}
