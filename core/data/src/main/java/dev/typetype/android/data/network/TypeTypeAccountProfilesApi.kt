package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.AccountProfileDto
import dev.typetype.android.data.network.dto.AccountProfilesResponse
import dev.typetype.android.data.network.dto.ProfileNameRequest
import dev.typetype.android.data.network.dto.ProfileSwitchResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface TypeTypeAccountProfilesApi {
    @GET("profiles")
    suspend fun accountProfiles(): Response<AccountProfilesResponse>

    @POST("profiles")
    suspend fun createAccountProfile(@Body body: ProfileNameRequest): Response<AccountProfileDto>

    @PUT("profiles/{profileId}")
    suspend fun renameAccountProfile(
        @Path("profileId") profileId: String,
        @Body body: ProfileNameRequest,
    ): Response<AccountProfileDto>

    @POST("profiles/{profileId}/default")
    suspend fun setDefaultAccountProfile(@Path("profileId") profileId: String): Response<AccountProfileDto>

    @DELETE("profiles/{profileId}")
    suspend fun deleteAccountProfile(@Path("profileId") profileId: String): Response<Unit>

    @POST("profiles/{profileId}/switch")
    suspend fun switchAccountProfile(
        @Header("Authorization") authorization: String,
        @Path("profileId") profileId: String,
    ): Response<ProfileSwitchResponse>
}
