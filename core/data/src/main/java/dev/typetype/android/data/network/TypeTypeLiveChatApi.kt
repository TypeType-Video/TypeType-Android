package dev.typetype.android.data.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Streaming

interface TypeTypeLiveChatApi {
    @Streaming
    @GET("live-chat")
    suspend fun liveChat(
        @Query("url") videoUrl: String,
        @Header("Accept") accept: String = "text/event-stream, application/json",
    ): Response<ResponseBody>
}
