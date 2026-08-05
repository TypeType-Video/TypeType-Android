package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.YoutubeRemoteBrowserStartRequest
import dev.typetype.android.data.network.dto.YoutubeRemoteBrowserStartResponse
import dev.typetype.android.data.network.dto.YoutubeSessionStatusResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface TypeTypeYoutubeSessionApi {
    @GET("youtube-session/status")
    suspend fun youtubeSessionStatus(): Response<YoutubeSessionStatusResponse>

    @POST("youtube-session/browser/start")
    suspend fun startYoutubeRemoteBrowser(
        @Body body: YoutubeRemoteBrowserStartRequest,
    ): Response<YoutubeRemoteBrowserStartResponse>

    @DELETE("youtube-session/browser/{sessionId}")
    suspend fun cancelYoutubeRemoteBrowser(
        @Path("sessionId") sessionId: String,
    ): Response<Unit>

    @DELETE("youtube-session")
    suspend fun disconnectYoutubeSession(): Response<Unit>
}
