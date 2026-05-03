package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.GuestResponse
import dev.typetype.android.data.network.dto.HealthResponse
import dev.typetype.android.data.network.dto.HomeRecommendationsResponse
import dev.typetype.android.data.network.dto.InstanceResponse
import dev.typetype.android.data.network.dto.LoginRequest
import dev.typetype.android.data.network.dto.RefreshRequest
import dev.typetype.android.data.network.dto.RegisterRequest
import dev.typetype.android.data.network.dto.SessionResponse
import dev.typetype.android.data.network.dto.UserProfile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface TypeTypeApi {

    @GET("health")
    suspend fun health(): Response<HealthResponse>

    @GET("instance")
    suspend fun instance(): Response<InstanceResponse>

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

    @GET("recommendations/home")
    suspend fun homeRecommendations(
        @Query("service") service: Int = 0,
        @Query("limit") limit: Int = 30,
        @Query("intent") intent: String? = null,
        @Query("cursor") cursor: String? = null,
    ): Response<HomeRecommendationsResponse>
}
