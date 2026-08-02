package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.ChannelPageRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChannelPageContractTest {
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
    fun channelPageUsesPostBodyForOpaqueContinuation() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"name":"Channel","description":"","avatarUrl":"","bannerUrl":"","subscriberCount":0,"isVerified":false,"videos":[],"nextpage":"next"}""",
                ),
        )
        val cursor = "opaque-" + "x".repeat(9_000)

        val response = api.channel(
            ChannelPageRequest(
                url = "https://youtube.com/@channel",
                nextpage = cursor,
                sort = "latest",
            ),
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/channel/page", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"url\":\"https://youtube.com/@channel\""))
        assertTrue(body.contains("\"nextpage\":\"$cursor\""))
        assertTrue(body.contains("\"sort\":\"latest\""))
        assertEquals("next", response.body()?.nextpage)
    }

    @Test
    fun channelPlaylistsUsesIndependentGetContinuation() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"playlists":[{"id":"pl","title":"Playlist","url":"https://youtube.com/playlist?list=pl","streamCount":12}],"nextpage":"more"}""",
                ),
        )
        val cursor = "playlist-" + "y".repeat(1_000)

        val response = api.channelPlaylists(
            url = "https://youtube.com/@channel",
            nextpage = cursor,
        )

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/channel/playlists", request.requestUrl?.encodedPath)
        assertEquals("https://youtube.com/@channel", request.requestUrl?.queryParameter("url"))
        assertEquals(cursor, request.requestUrl?.queryParameter("nextpage"))
        assertEquals("Playlist", response.body()?.playlists?.single()?.title)
        assertEquals("more", response.body()?.nextpage)
    }
}
