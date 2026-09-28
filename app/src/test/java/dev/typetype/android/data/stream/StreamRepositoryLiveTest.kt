package dev.typetype.android.data.stream

import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.account.AccountScopeProvider
import dev.typetype.android.data.network.AlwaysAvailablePlaybackNetworkObserver
import dev.typetype.android.data.network.dto.AudioStreamItem
import dev.typetype.android.data.network.dto.StreamResponse
import dev.typetype.android.data.network.dto.VideoStreamItem
import dev.typetype.android.domain.server.Server
import dev.typetype.android.domain.server.ServerRepository
import dev.typetype.android.domain.stream.StreamPlaybackContract
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class StreamRepositoryLiveTest {
    @Test
    fun `youtube live hls response resolves the signed server manifest`() = runBlocking {
        val repository = repository(
            response(
                isLive = true,
                hasLiveManifest = true,
                hlsUrl = LIVE_HLS_URL,
            ),
        )

        val stream = repository.loadPlaybackStream(VIDEO_URL).getOrThrow()

        assertEquals(StreamPlaybackContract.ProviderMedia, stream.playbackContract)
        assertEquals("$BASE_URL$LIVE_HLS_PATH", stream.hlsUrl)
        assertNull(stream.serverSabrManifestUrl)
        assertNull(stream.serverHlsManifestUrl)
        assertNull(stream.serverDashManifestUrl)
        assertTrue(stream.isLive)
        assertTrue(stream.sabrVideoStreams.isEmpty())
    }

    @Test
    fun `youtube live response without a server manifest is rejected`() = runBlocking {
        val repository = repository(
            response(
                isLive = true,
                hasLiveManifest = true,
                hlsUrl = "https://media.example/live.m3u8",
            ),
        )

        val failure = repository.loadPlaybackStream(VIDEO_URL).exceptionOrNull()

        assertEquals("youtube_sabr_unavailable", (failure as CodedFailure).failureCode)
    }

    @Test
    fun `youtube video without sabr or live manifest is rejected`() = runBlocking {
        val repository = repository(response(isLive = false, hasLiveManifest = false, hlsUrl = ""))

        val failure = repository.loadPlaybackStream(VIDEO_URL).exceptionOrNull()

        assertEquals("youtube_sabr_unavailable", (failure as CodedFailure).failureCode)
    }

    @Test
    fun `youtube vod keeps the sabr contract and drops provider manifests`() = runBlocking {
        val repository = repository(
            response(
                isLive = false,
                hasLiveManifest = false,
                hlsUrl = "",
                sabrFormats = true,
            ),
        )

        val stream = repository.loadPlaybackStream(VIDEO_URL).getOrThrow()

        assertEquals(StreamPlaybackContract.ServerSabr, stream.playbackContract)
        assertNull(stream.hlsUrl)
        assertEquals("$BASE_URL${SABR_MANIFEST_PATH.substringAfter('/')}", stream.serverSabrManifestUrl)
    }

    private fun repository(response: StreamResponse) = StreamRepositoryImpl(
        remoteSource = FixedStreamRemoteSource(response),
        activeAccountScope = FixedAccountScope,
        serverRepository = FixedServerRepository,
        networkMonitor = AlwaysAvailablePlaybackNetworkObserver,
    )

    private class FixedStreamRemoteSource(
        private val response: StreamResponse,
    ) : StreamRemoteSource {
        override suspend fun load(
            scope: AccountScope,
            videoUrl: String,
            provider: StreamProvider,
            playbackBootstrap: Boolean,
        ): Response<StreamResponse> = Response.success(response)
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
        isLive: Boolean,
        hasLiveManifest: Boolean,
        hlsUrl: String,
        sabrFormats: Boolean = false,
    ) = StreamResponse(
        id = "video",
        title = "Video",
        uploaderName = "Channel",
        uploaderUrl = "/channel",
        uploaderAvatarUrl = "",
        thumbnailUrl = "",
        description = "",
        duration = if (isLive) 0L else 60L,
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
        streamType = if (isLive) "LIVE_STREAM" else "VIDEO_STREAM",
        isShortFormContent = false,
        requiresMembership = false,
        isLive = isLive,
        hasLiveManifest = hasLiveManifest,
        startPosition = 0L,
        hlsUrl = hlsUrl,
        dashMpdUrl = "",
        videoOnlyStreams = if (sabrFormats) listOf(videoStream()) else emptyList(),
        audioStreams = if (sabrFormats) listOf(audioStream()) else emptyList(),
    )

    private fun videoStream() = VideoStreamItem(
        url = "",
        mimeType = "video/mp4",
        format = "MPEG_4",
        resolution = "720p",
        codec = "avc1.64001f",
        isVideoOnly = true,
        itag = 136,
        width = 1280,
        height = 720,
        fps = 30,
        contentLength = 1L,
        deliveryMethod = "sabr",
        manifestUrl = SABR_MANIFEST_PATH,
    )

    private fun audioStream() = AudioStreamItem(
        url = "",
        mimeType = "audio/mp4",
        format = "MPEG_4",
        codec = "mp4a.40.2",
        itag = 140,
        contentLength = 1L,
        isOriginal = true,
        deliveryMethod = "sabr",
        manifestUrl = SABR_MANIFEST_PATH,
    )

    private companion object {
        const val SERVER_ID = "server"
        const val ACCOUNT_ID = "account"
        const val BASE_URL = "https://instance.example/api/"
        const val VIDEO_URL = "https://www.youtube.com/watch?v=video"
        const val SABR_MANIFEST_PATH = "/sabr/manifest/video"
        const val LIVE_HLS_PATH = "streams/hls-manifest?token=signed"
        const val LIVE_HLS_URL = "/$LIVE_HLS_PATH"
    }
}
