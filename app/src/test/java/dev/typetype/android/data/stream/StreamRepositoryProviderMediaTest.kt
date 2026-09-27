package dev.typetype.android.data.stream

import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.account.AccountScopeProvider
import dev.typetype.android.data.network.AlwaysAvailablePlaybackNetworkObserver
import dev.typetype.android.data.network.dto.AudioStreamItem
import dev.typetype.android.data.network.dto.StreamResponse
import dev.typetype.android.data.network.dto.VideoStreamItem
import dev.typetype.android.domain.server.Server
import dev.typetype.android.domain.server.ServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Response

class StreamRepositoryProviderMediaTest {
    @Test
    fun `niconico media handles resolve and keep hls playback container`() = runBlocking {
        val repository = repository(
            NICO_URL,
            response(
                hlsVideoHandles = true,
                videoHandles = listOf(HANDLE_A, HANDLE_B),
                audioHandles = listOf(HANDLE_AUDIO),
            ),
        )

        val stream = repository.loadPlaybackStream(NICO_URL).getOrThrow()
        val video = stream.videoOnlyStreams.first()

        assertEquals("$BASE_URL${HANDLE_A.substringAfter('/')}", video.url)
        assertEquals("application/vnd.apple.mpegurl", video.playbackMimeType)
        assertEquals(360, video.height)
        assertEquals("$BASE_URL${HANDLE_AUDIO.substringAfter('/')}", stream.audioStreams.first().url)
        assertNull(stream.hlsUrl)
    }

    @Test
    fun `bilibili progressive handles resolve without a playback container`() = runBlocking {
        val repository = repository(
            BILI_URL,
            response(
                hlsVideoHandles = false,
                videoHandles = listOf(HANDLE_A),
                audioHandles = listOf(HANDLE_AUDIO),
                height = 384,
                codec = "avc1.64001E",
            ),
        )

        val stream = repository.loadPlaybackStream(BILI_URL).getOrThrow()

        assertEquals("$BASE_URL${HANDLE_A.substringAfter('/')}", stream.videoOnlyStreams.first().url)
        assertNull(stream.videoOnlyStreams.first().playbackMimeType)
        assertEquals(384, stream.videoOnlyStreams.first().height)
    }

    @Test
    fun `absolute provider media url is preserved`() = runBlocking {
        val repository = repository(
            NICO_URL,
            response(
                hlsVideoHandles = false,
                videoHandles = listOf(),
                audioHandles = listOf(),
                absoluteUrl = "https://cdn.example/media.m3u8",
            ),
        )

        val stream = repository.loadPlaybackStream(NICO_URL).getOrThrow()

        assertEquals("https://cdn.example/media.m3u8", stream.videoOnlyStreams.first().url)
    }

    private fun repository(videoUrl: String, response: StreamResponse) = StreamRepositoryImpl(
        remoteSource = FixedStreamRemoteSource(videoUrl, response),
        activeAccountScope = FixedAccountScope,
        serverRepository = FixedServerRepository,
        networkMonitor = AlwaysAvailablePlaybackNetworkObserver,
    )

    private class FixedStreamRemoteSource(
        private val expectedUrl: String,
        private val response: StreamResponse,
    ) : StreamRemoteSource {
        override suspend fun load(
            scope: AccountScope,
            videoUrl: String,
            provider: StreamProvider,
            playbackBootstrap: Boolean,
        ): Response<StreamResponse> {
            check(videoUrl == expectedUrl)
            return Response.success(response)
        }
    }

    private object FixedAccountScope : AccountScopeProvider {
        private val scope = AccountScope(SERVER_ID, ACCOUNT_ID)

        override fun observe(): Flow<AccountScope?> = flowOf(scope)

        override suspend fun require(): AccountScope = scope

        override suspend fun verify(expected: AccountScope) {
            check(expected == scope)
        }
    }

    private object FixedServerRepository : ServerRepository {
        private val server = Server(SERVER_ID, BASE_URL, "Instance", 0L)

        override fun observeServers(): Flow<List<Server>> = flowOf(listOf(server))
        override fun observeCurrentServer(): Flow<Server?> = flowOf(server)
        override suspend fun getServer(id: String): Server? = server.takeIf { id == SERVER_ID }
        override suspend fun addServer(server: Server) = Unit
        override suspend fun deleteServer(id: String) = Unit
        override suspend fun setCurrentServer(id: String) = Unit
        override suspend fun clearCurrentServer() = Unit
    }

    private fun response(
        hlsVideoHandles: Boolean,
        videoHandles: List<String>,
        audioHandles: List<String>,
        height: Int = 0,
        codec: String? = null,
        absoluteUrl: String? = null,
    ) = StreamResponse(
        id = "video",
        title = "Video",
        uploaderName = "Channel",
        uploaderUrl = "/channel",
        uploaderAvatarUrl = "",
        thumbnailUrl = "",
        description = "",
        duration = 320L,
        viewCount = 1L,
        likeCount = 0L,
        dislikeCount = 0L,
        uploadDate = "",
        uploaded = 0L,
        uploaderSubscriberCount = 1L,
        uploaderVerified = false,
        category = "",
        license = "",
        visibility = "public",
        streamType = "VIDEO_STREAM",
        isShortFormContent = false,
        requiresMembership = false,
        startPosition = 0L,
        hlsUrl = "",
        dashMpdUrl = "",
        videoOnlyStreams = (videoHandles + listOfNotNull(absoluteUrl)).map { handle ->
            videoStream(handle, hlsVideoHandles, height, codec)
        },
        audioStreams = audioHandles.map { handle -> audioStream(handle) },
    )

    private fun videoStream(
        url: String,
        hls: Boolean,
        height: Int,
        codec: String?,
    ) = VideoStreamItem(
        url = url,
        mimeType = "video/mp4",
        format = "MPEG-4",
        resolution = "360p",
        codec = codec,
        isVideoOnly = true,
        itag = -1,
        width = 0,
        height = height,
        fps = 0,
        contentLength = 0L,
        deliveryMethod = if (hls) "hls" else "progressive",
    )

    private fun audioStream(url: String) = AudioStreamItem(
        url = url,
        mimeType = "audio/mp4",
        format = "m4a",
        codec = null,
        itag = -1,
        contentLength = 0L,
        isOriginal = false,
        deliveryMethod = "progressive",
    )

    private companion object {
        const val SERVER_ID = "server"
        const val ACCOUNT_ID = "account"
        const val BASE_URL = "https://instance.example/api/"
        const val NICO_URL = "https://www.nicovideo.jp/watch/sm9"
        const val BILI_URL = "https://www.bilibili.com/video/BV1xx411c7mD"
        const val HANDLE_A = "/media/m1_WcTj0wQ0RcWaByyoMvP8E0ak"
        const val HANDLE_B = "/media/m1_vyvt1J_-qiWsTYEyHrEP_q-D"
        const val HANDLE_AUDIO = "/media/m1_X9jjkzoAVheGT2QYaOYSR08A"
    }
}
