package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.RecommenderFeedbackDto
import dev.typetype.android.data.network.dto.RecommenderFeedbackRequestDto
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TypeTypeRecommenderApi {
    @GET("recommendations/settings")
    suspend fun recommenderSettings(): Response<JsonObject>

    @PUT("recommendations/settings")
    suspend fun updateRecommenderSettings(@Body settings: JsonObject): Response<JsonObject>

    @POST("recommendations/feedback")
    suspend fun sendRecommenderFeedback(
        @Body feedback: RecommenderFeedbackRequestDto,
    ): Response<RecommenderFeedbackDto>

    @GET("recommendations/algorithms")
    suspend fun recommenderAlgorithms(
        @Query("locale") locale: String,
        @Query("timezone") timezone: String,
    ): Response<JsonObject>

    @GET("recommendations/profile")
    suspend fun recommenderProfile(
        @Query("locale") locale: String,
        @Query("timezone") timezone: String,
    ): Response<JsonObject>

    @GET("recommendations/data")
    suspend fun recommenderData(): Response<JsonArray>

    @GET("recommendations/last-home")
    suspend fun recommenderLastHome(): Response<JsonObject>

    @GET("recommendations/feedback")
    suspend fun recommenderFeedback(): Response<JsonArray>

    @DELETE("recommendations/feedback/{id}")
    suspend fun deleteRecommenderFeedback(@Path("id") id: String): Response<Unit>
}
