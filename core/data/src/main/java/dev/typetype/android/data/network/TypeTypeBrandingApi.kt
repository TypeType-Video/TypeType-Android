package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.DeArrowDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TypeTypeBrandingApi {
    @GET("dearrow")
    suspend fun deArrow(@Query("videoId") videoId: String): Response<DeArrowDto>
}
