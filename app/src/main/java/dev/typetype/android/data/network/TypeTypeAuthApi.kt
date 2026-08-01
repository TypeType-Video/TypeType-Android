package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.GuestResponse
import dev.typetype.android.data.network.dto.LoginRequest
import dev.typetype.android.data.network.dto.OidcCallbackRequest
import dev.typetype.android.data.network.dto.OidcCallbackResponse
import dev.typetype.android.data.network.dto.OidcStartResponse
import dev.typetype.android.data.network.dto.OidcStatusResponse
import dev.typetype.android.data.network.dto.RefreshRequest
import dev.typetype.android.data.network.dto.ResetPasswordRequest
import dev.typetype.android.data.network.dto.RegisterRequest
import dev.typetype.android.data.network.dto.SessionResponse
import dev.typetype.android.data.network.dto.UserProfile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface TypeTypeAuthApi {
    @POST("auth/guest")
    suspend fun guest(): Response<GuestResponse>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<SessionResponse>

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<SessionResponse>

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<SessionResponse>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): Response<Unit>

    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") authorization: String? = null): Response<Unit>

    @GET("auth/oidc/start")
    suspend fun startOidc(
        @Query("redirectUri") redirectUri: String,
        @Query("returnTo") returnTo: String = "/",
    ): Response<OidcStartResponse>

    @GET("auth/oidc/status")
    suspend fun oidcStatus(): Response<OidcStatusResponse>

    @POST("auth/oidc/callback")
    suspend fun finishOidc(@Body body: OidcCallbackRequest): Response<OidcCallbackResponse>

    @GET("auth/me")
    suspend fun me(@Header("Authorization") authorization: String? = null): Response<UserProfile>
}
