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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class StreamRepositoryPrefetchTest {
    @Test
    fun `prefetched short activation downloads zero additional response bytes`() = runBlocking {
        val remote = CountingStreamRemoteSource(response())
        val repository = StreamRepositoryImpl(
            remoteSource = remote,
            activeAccountScope = FixedAccountScope,
            serverRepository = FixedServerRepository,
            networkMonitor = AlwaysAvailablePlaybackNetworkObserver,
        )

        repository.prefetchPlaybackStream(VIDEO_URL).getOrThrow()
        repository.prefetchPlaybackStream(VIDEO_URL).getOrThrow()
        val bytesAfterPrefetch = remote.downloadedBytes
        val requestsAfterPrefetch = remote.requestCount

        val stream = repository.loadPlaybackStream(VIDEO_URL).getOrThrow()

        assertEquals("video", stream.id)
        assertEquals(requestsAfterPrefetch, remote.requestCount)
        assertEquals(bytesAfterPrefetch, remote.downloadedBytes)
        assertEquals(1, remote.requestCount)
        assertEquals(EXPECTED_RESPONSE_BYTES, remote.responseBytes)
        assertEquals(remote.responseBytes, remote.downloadedBytes)
    }

    private class CountingStreamRemoteSource(
        private val response: StreamResponse,
    ) : StreamRemoteSource {
        val responseBytes = Json.encodeToString(response).encodeToByteArray().size.toLong()
        var requestCount = 0
            private set
        var downloadedBytes = 0L
            private set

        override suspend fun load(
            scope: AccountScope,
            videoUrl: String,
            provider: StreamProvider,
            playbackBootstrap: Boolean,
        ): Response<StreamResponse> {
            requestCount += 1
            downloadedBytes += responseBytes
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

    private fun response() = StreamResponse(
        id = "video",
        title = "Short",
        uploaderName = "Channel",
        uploaderUrl = "/channel",
        uploaderAvatarUrl = "",
        thumbnailUrl = "",
        description = "",
        duration = 30L,
        viewCount = 1L,
        likeCount = 1L,
        dislikeCount = 0L,
        uploadDate = "",
        uploaded = 0L,
        uploaderSubscriberCount = 1L,
        uploaderVerified = false,
        category = "",
        license = "",
        visibility = "public",
        streamType = "VIDEO_STREAM",
        isShortFormContent = true,
        requiresMembership = false,
        startPosition = 0L,
        hlsUrl = "",
        dashMpdUrl = "",
        videoOnlyStreams = listOf(videoStream()),
        audioStreams = listOf(audioStream()),
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
        manifestUrl = "/api/sabr/playback/video/manifest",
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
        manifestUrl = "/api/sabr/playback/video/manifest",
    )

    private companion object {
        const val SERVER_ID = "server"
        const val ACCOUNT_ID = "account"
        const val BASE_URL = "https://instance.example/api/"
        const val VIDEO_URL = "https://www.youtube.com/watch?v=video"
        const val EXPECTED_RESPONSE_BYTES = 937L
    }
}
