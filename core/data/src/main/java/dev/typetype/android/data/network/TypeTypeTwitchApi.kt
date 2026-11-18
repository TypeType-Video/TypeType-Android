package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.TwitchAccountStatusDto
import dev.typetype.android.data.network.dto.TwitchChannelDto
import dev.typetype.android.data.network.dto.TwitchChatAssetsDto
import dev.typetype.android.data.network.dto.TwitchChatHighlightsDto
import dev.typetype.android.data.network.dto.TwitchClipDto
import dev.typetype.android.data.network.dto.TwitchClipPlaybackDto
import dev.typetype.android.data.network.dto.TwitchDeviceCodeDto
import dev.typetype.android.data.network.dto.TwitchDevicePollDto
import dev.typetype.android.data.network.dto.TwitchDevicePollRequest
import dev.typetype.android.data.network.dto.TwitchFollowStateDto
import dev.typetype.android.data.network.dto.TwitchGameDto
import dev.typetype.android.data.network.dto.TwitchGameStreamsDto
import dev.typetype.android.data.network.dto.TwitchLiveDto
import dev.typetype.android.data.network.dto.TwitchPageDto
import dev.typetype.android.data.network.dto.TwitchStreamDto
import dev.typetype.android.data.network.dto.TwitchVideoChapterDto
import dev.typetype.android.data.network.dto.TwitchVideoCommentDto
import dev.typetype.android.data.network.dto.TwitchVideoDto
import dev.typetype.android.data.network.dto.TwitchVideoPlaybackDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TypeTypeTwitchApi {
    @GET("twitch/streams")
    suspend fun twitchStreams(@Query("after") after: String? = null, @Query("first") first: Int = TWITCH_PAGE): Response<TwitchPageDto<TwitchStreamDto>>

    @GET("twitch/games")
    suspend fun twitchGames(@Query("after") after: String? = null, @Query("first") first: Int = TWITCH_PAGE): Response<TwitchPageDto<TwitchGameDto>>

    @GET("twitch/games/{slug}/streams")
    suspend fun twitchGameStreams(
        @Path("slug") slug: String,
        @Query("after") after: String? = null,
        @Query("first") first: Int = TWITCH_PAGE,
    ): Response<TwitchGameStreamsDto>

    @GET("twitch/tags/{tag}/streams")
    suspend fun twitchTagStreams(
        @Path("tag") tag: String,
        @Query("after") after: String? = null,
        @Query("first") first: Int = TWITCH_PAGE,
    ): Response<TwitchPageDto<TwitchStreamDto>>

    @GET("twitch/channels/{login}")
    suspend fun twitchChannel(@Path("login") login: String): Response<TwitchChannelDto>

    @GET("twitch/channels/{login}/videos")
    suspend fun twitchChannelVideos(
        @Path("login") login: String,
        @Query("after") after: String? = null,
        @Query("first") first: Int = TWITCH_PAGE,
    ): Response<TwitchPageDto<TwitchVideoDto>>

    @GET("twitch/channels/{login}/clips")
    suspend fun twitchChannelClips(
        @Path("login") login: String,
        @Query("after") after: String? = null,
        @Query("first") first: Int = TWITCH_PAGE,
    ): Response<TwitchPageDto<TwitchClipDto>>

    @GET("twitch/live/{login}")
    suspend fun twitchLive(@Path("login") login: String): Response<TwitchLiveDto>

    @GET("twitch/videos/{id}")
    suspend fun twitchVideo(@Path("id") id: String): Response<TwitchVideoPlaybackDto>

    @GET("twitch/videos/{id}/chapters")
    suspend fun twitchVideoChapters(@Path("id") id: String): Response<List<TwitchVideoChapterDto>>

    @GET("twitch/videos/{id}/comments")
    suspend fun twitchVideoComments(
        @Path("id") id: String,
        @Query("offset") offset: Long? = null,
        @Query("cursor") cursor: String? = null,
    ): Response<TwitchPageDto<TwitchVideoCommentDto>>

    @GET("twitch/clips/{slug}")
    suspend fun twitchClip(@Path("slug") slug: String): Response<TwitchClipPlaybackDto>

    @GET("twitch/search")
    suspend fun twitchSearchChannels(
        @Query("q") query: String,
        @Query("type") type: String = "channels",
        @Query("after") after: String? = null,
        @Query("first") first: Int = TWITCH_PAGE,
    ): Response<TwitchPageDto<TwitchChannelDto>>

    @GET("twitch/search")
    suspend fun twitchSearchStreams(
        @Query("q") query: String,
        @Query("type") type: String = "streams",
        @Query("after") after: String? = null,
        @Query("first") first: Int = TWITCH_PAGE,
    ): Response<TwitchPageDto<TwitchStreamDto>>

    @GET("twitch/account")
    suspend fun twitchAccount(): Response<TwitchAccountStatusDto>

    @POST("twitch/account/device")
    suspend fun twitchStartDevice(): Response<TwitchDeviceCodeDto>

    @POST("twitch/account/poll")
    suspend fun twitchPollDevice(@Body body: TwitchDevicePollRequest): Response<TwitchDevicePollDto>

    @DELETE("twitch/account")
    suspend fun twitchUnlink(): Response<Unit>

    @GET("twitch/me/live")
    suspend fun twitchFollowedLives(@Query("after") after: String? = null): Response<TwitchPageDto<TwitchStreamDto>>

    @GET("twitch/me/videos")
    suspend fun twitchFollowedVideos(@Query("after") after: String? = null): Response<TwitchPageDto<TwitchVideoDto>>

    @GET("twitch/me/follows")
    suspend fun twitchFollows(@Query("after") after: String? = null): Response<TwitchPageDto<TwitchChannelDto>>

    @GET("twitch/me/follows/{login}")
    suspend fun twitchFollowState(@Path("login") login: String): Response<TwitchFollowStateDto>

    @PUT("twitch/me/follows/{login}")
    suspend fun twitchFollow(@Path("login") login: String): Response<Unit>

    @DELETE("twitch/me/follows/{login}")
    suspend fun twitchUnfollow(@Path("login") login: String): Response<Unit>

    @GET("twitch/chat/assets")
    suspend fun twitchChatAssets(): Response<TwitchChatAssetsDto>

    @GET("twitch/chat/{login}/assets")
    suspend fun twitchChannelChatAssets(@Path("login") login: String): Response<TwitchChatAssetsDto>

    @GET("twitch/chat/{login}/highlights")
    suspend fun twitchChatHighlights(@Path("login") login: String): Response<TwitchChatHighlightsDto>
}

const val TWITCH_PAGE = 24
