package dev.typetype.android.data.network

import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface TypeTypeRecommenderApi {
    @GET("recommendations/settings")
    suspend fun recommenderSettings(): Response<JsonObject>

    @PUT("recommendations/settings")
    suspend fun updateRecommenderSettings(@Body settings: JsonObject): Response<JsonObject>
}
