package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.CommentsPageResponse
import dev.typetype.android.data.network.dto.InstanceResponse
import dev.typetype.android.data.network.dto.SabrPlaybackPositionRequestDto
import dev.typetype.android.data.network.dto.SabrPlaybackPositionResponseDto
import dev.typetype.android.data.network.dto.SabrPlaybackRequest
import dev.typetype.android.data.network.dto.SabrPlaybackResponse
import dev.typetype.android.data.network.dto.SabrPlaybackWindowRequestDto
import dev.typetype.android.data.network.dto.SabrPlaybackWindowResponseDto
import dev.typetype.android.data.network.dto.StreamResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface TypeTypeMediaApi {
    @GET("instance")
    suspend fun instance(): Response<InstanceResponse>

    @GET("streams")
    suspend fun streams(@Query("url") videoUrl: String): Response<StreamResponse>

    @GET("streams/youtube/sabr")
    suspend fun youtubeSabrStreams(@Query("url") videoUrl: String): Response<StreamResponse>

    @GET("streams/youtube/sabr/bootstrap")
    suspend fun youtubeSabrBootstrap(@Query("url") videoUrl: String): Response<StreamResponse>

    @GET("streams/niconico")
    suspend fun nicoNicoStreams(@Query("url") videoUrl: String): Response<StreamResponse>

    @GET("streams/bilibili")
    suspend fun biliBiliStreams(@Query("url") videoUrl: String): Response<StreamResponse>

    @POST("sabr/playback/{videoId}")
    suspend fun createSabrPlayback(
        @Path("videoId") videoId: String,
        @Body body: SabrPlaybackRequest,
    ): Response<SabrPlaybackResponse>

    @POST("sabr/playback/{sessionId}/seek")
    suspend fun seekSabrPlayback(
        @Path("sessionId") sessionId: String,
        @Body body: SabrPlaybackRequest,
    ): Response<SabrPlaybackResponse>

    @POST("sabr/playback/{sessionId}/position")
    suspend fun updateSabrPlaybackPosition(
        @Path("sessionId") sessionId: String,
        @Body body: SabrPlaybackPositionRequestDto,
    ): Response<SabrPlaybackPositionResponseDto>

    @POST("sabr/playback/{sessionId}/prefetch")
    suspend fun prefetchSabrPlayback(
        @Path("sessionId") sessionId: String,
        @Body body: SabrPlaybackWindowRequestDto,
    ): Response<SabrPlaybackWindowResponseDto>

    @POST("sabr/playback/{sessionId}/segments")
    suspend fun sabrPlaybackSegments(
        @Path("sessionId") sessionId: String,
        @Body body: SabrPlaybackWindowRequestDto,
    ): Response<SabrPlaybackWindowResponseDto>

    @POST("sabr/playback/{sessionId}/window")
    suspend fun sabrPlaybackWindow(
        @Path("sessionId") sessionId: String,
        @Body body: SabrPlaybackWindowRequestDto,
    ): Response<SabrPlaybackWindowResponseDto>

    @GET("comments")
    suspend fun comments(
        @Query("url") videoUrl: String,
        @Query("nextpage") nextpage: String? = null,
    ): Response<CommentsPageResponse>

    @GET("comments/replies")
    suspend fun commentReplies(
        @Query("url") videoUrl: String,
        @Query("repliesPage") repliesPage: String,
    ): Response<CommentsPageResponse>

    @GET
    suspend fun subtitle(@Url url: String): Response<ResponseBody>
}
