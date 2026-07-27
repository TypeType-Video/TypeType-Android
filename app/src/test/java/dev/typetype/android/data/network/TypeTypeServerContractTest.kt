package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.AddHistoryRequest
import dev.typetype.android.data.network.dto.AddWatchLaterRequest
import dev.typetype.android.data.network.dto.SabrPlaybackPositionRequestDto
import dev.typetype.android.data.network.dto.SabrPlaybackRequest
import dev.typetype.android.data.network.dto.SabrPlaybackWindowRequestDto
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TypeTypeServerContractTest {
    private lateinit var server: MockWebServer
    private lateinit var api: TypeTypeApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = RetrofitFactory(
            sessionClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true },
        ).create(server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun aggregateApiKeepsInheritedDiscoveryAndMediaRoutes() = runBlocking {
        server.enqueue(errorResponse())
        server.enqueue(errorResponse())

        api.health()
        api.youtubeSabrStreams(VIDEO_URL)

        assertEquals("/health", server.takeRequest().path)
        assertEquals(
            "/streams/youtube/sabr?url=${encode(VIDEO_URL)}",
            server.takeRequest().path,
        )
    }

    @Test
    fun providerSpecificStreamRoutesMatchPinnedServerContract() = runBlocking {
        repeat(2) { server.enqueue(errorResponse()) }

        api.nicoNicoStreams(NICONICO_URL)
        api.biliBiliStreams(BILIBILI_URL)

        assertEquals("/streams/niconico?url=${encode(NICONICO_URL)}", server.takeRequest().path)
        assertEquals("/streams/bilibili?url=${encode(BILIBILI_URL)}", server.takeRequest().path)
    }

    @Test
    fun sharedSabrRoutesMatchPinnedServerContract() = runBlocking {
        repeat(6) { server.enqueue(errorResponse()) }

        api.createSabrPlayback(
            "video",
            SabrPlaybackRequest(videoItag = 137, audioItag = 140),
        )
        api.seekSabrPlayback(
            "session",
            SabrPlaybackRequest(videoItag = 137, audioItag = 140, playerTimeMs = 42_000L),
        )
        api.updateSabrPlaybackPosition(
            "session",
            SabrPlaybackPositionRequestDto(0, 42_000, 137, 140),
        )
        api.prefetchSabrPlayback(
            "session",
            SabrPlaybackWindowRequestDto(0, 42_000, 137, 140),
        )
        api.sabrPlaybackSegments(
            "session",
            SabrPlaybackWindowRequestDto(0, 42_000, 137, 140),
        )
        api.sabrPlaybackWindow(
            "session",
            SabrPlaybackWindowRequestDto(0, 42_000, 137, 140),
        )

        assertEquals("/sabr/playback/video", server.takeRequest().path)
        assertEquals("/sabr/playback/session/seek", server.takeRequest().path)
        assertEquals("/sabr/playback/session/position", server.takeRequest().path)
        assertEquals("/sabr/playback/session/prefetch", server.takeRequest().path)
        assertEquals("/sabr/playback/session/segments", server.takeRequest().path)
        assertEquals("/sabr/playback/session/window", server.takeRequest().path)
    }

    @Test
    fun subscriptionsFeedUsesTheOpaqueCursorContract() = runBlocking {
        server.enqueue(
            jsonResponse(
                """{"videos":[],"nextpage":"opaque","generation":7,"generatedAt":6,"refreshing":true}""",
                200,
            ),
        )

        val response = api.subscriptionsFeed(limit = 12, cursor = "opaque")

        assertEquals("/subscriptions/feed?limit=12&cursor=opaque", server.takeRequest().path)
        assertEquals(7L, response.body()?.generation)
        assertTrue(response.body()?.refreshing == true)
    }

    @Test
    fun contractRevisionIsPinnedToInspectedDevelopmentSource() {
        assertEquals(40, SERVER_CONTRACT_REVISION.length)
        assertEquals("d813f59", SERVER_CONTRACT_REVISION.take(7))
    }

    @Test
    fun subscriptionFeedContractIsPinnedToTheDeployedBetaSource() {
        assertEquals("0c38bae", SUBSCRIPTION_FEED_CONTRACT_REVISION.take(7))
    }

    @Test
    fun dedicatedCollectionRoutesKeepMetadataAndServerHistoryIdentity() = runBlocking {
        server.enqueue(jsonResponse("{}", 201))
        server.enqueue(
            jsonResponse(
                """{"id":"history-id","url":"$VIDEO_URL","title":"Video","thumbnail":"thumb","channelName":"Channel","channelUrl":"channel","duration":42,"progress":0,"watchedAt":10}""",
                201,
            ),
        )

        api.addWatchLater(
            AddWatchLaterRequest(
                url = VIDEO_URL,
                title = "Video",
                thumbnail = "thumb",
                duration = 42L,
                channelName = "Channel",
                channelUrl = "channel",
                channelAvatar = "avatar",
                viewCount = 7L,
            ),
        )
        val history = api.addHistory(
            AddHistoryRequest(
                url = VIDEO_URL,
                title = "Video",
                thumbnail = "thumb",
                duration = 42L,
                channelName = "Channel",
                channelUrl = "channel",
            ),
        ).body()

        val watchRequest = server.takeRequest()
        assertEquals("/watch-later", watchRequest.path)
        assertTrue(watchRequest.body.readUtf8().contains("\"channelAvatar\":\"avatar\""))
        assertEquals("/history", server.takeRequest().path)
        assertEquals("history-id", history?.id)
    }

    @Test
    fun collectionContractIsPinnedToFreshServerMain() {
        assertEquals("e885c20", COLLECTION_CONTRACT_REVISION.take(7))
    }

    @Test
    fun playlistSummaryAndDetailUseSeparateServerRoutes() = runBlocking {
        server.enqueue(
            jsonResponse(
                """[{"id":"saved","name":"Saved","videoCount":3,"createdAt":1}]""",
                200,
            ),
        )
        server.enqueue(
            jsonResponse(
                """{"id":"saved","name":"Saved","videoCount":1,"createdAt":1,"videos":[{"id":"item","url":"video","title":"Title","thumbnail":"thumb","duration":42,"position":0}]}""",
                200,
            ),
        )

        val summaries = requireNotNull(api.playlists().body())
        val detail = requireNotNull(api.playlist("saved").body())

        assertEquals(3, summaries.single().videoCount)
        assertTrue(summaries.single().videos.isEmpty())
        assertEquals("video", detail.videos.single().url)
        assertEquals("/playlists", server.takeRequest().path)
        assertEquals("/playlists/saved", server.takeRequest().path)
    }

    @Test
    fun historyPaginationKeepsOffsetLimitAndTotalCount() = runBlocking {
        server.enqueue(jsonResponse("[]", 200).setHeader("X-Total-Count", "125"))

        val response = api.history(limit = 60, offset = 60)

        assertEquals("125", response.headers()["X-Total-Count"])
        assertEquals("/history?limit=60&offset=60", server.takeRequest().path)
    }

    @Test
    fun settingsUpdateSendsOnlyTheChangedPrivacyField() = runBlocking {
        server.enqueue(jsonResponse("""{"disableWatchHistory":true,"autoplay":false}""", 200))

        val response = api.updateSettings(
            buildJsonObject { put("disableWatchHistory", true) },
        )

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/settings", request.path)
        assertEquals("""{"disableWatchHistory":true}""", request.body.readUtf8())
        assertTrue(requireNotNull(response.body()).disableWatchHistory)
    }

    private fun errorResponse(): MockResponse = MockResponse()
        .setResponseCode(422)
        .setHeader("Content-Type", "application/json")
        .setBody("{\"error\":\"contract fixture\"}")

    private fun jsonResponse(body: String, status: Int): MockResponse = MockResponse()
        .setResponseCode(status)
        .setHeader("Content-Type", "application/json")
        .setBody(body)

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")

    private companion object {
        const val SERVER_CONTRACT_REVISION = "d813f59f84c566cc2a9edf5b61156d820efe3910"
        const val SUBSCRIPTION_FEED_CONTRACT_REVISION = "0c38baee8fa37c313d676fc3df6c5d812081df32"
        const val COLLECTION_CONTRACT_REVISION = "e885c209279684b56edd8be06c6c8fdb6857a1a6"
        const val VIDEO_URL = "https://www.youtube.com/watch?v=video"
        const val NICONICO_URL = "https://www.nicovideo.jp/watch/sm9"
        const val BILIBILI_URL = "https://www.bilibili.com/video/BV1xx"
    }
}
