package dev.typetype.android.data.feed

import dev.typetype.android.data.network.RetrofitFactory
import dev.typetype.android.data.network.ServerResponseException
import dev.typetype.android.data.network.TypeTypeFeedApi
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class SubscriptionFeedRecoveryHarnessTest {
    private lateinit var server: MockWebServer
    private lateinit var api: TypeTypeFeedApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val client = OkHttpClient.Builder()
            .connectTimeout(250, TimeUnit.MILLISECONDS)
            .readTimeout(250, TimeUnit.MILLISECONDS)
            .build()
        api = RetrofitFactory(client, Json { ignoreUnknownKeys = true })
            .create(server.url("/").toString(), TypeTypeFeedApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun cachedContentSurvivesLossTimeoutAnd530UntilRecovery() = runBlocking {
        server.enqueue(readyResponse("cached", 7))
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        server.enqueue(
            jsonResponse(
                530,
                """{"error":"Upstream unavailable","code":"subscription_feed_upstream_unavailable"}""",
            ).setHeader("X-Request-ID", "request-530"),
        )
        server.enqueue(readyResponse("fresh", 8))
        var visibleVideos = load().videos.map { it.id }

        assertThrows(IOException::class.java) { runBlocking { load() } }
        assertEquals(listOf("cached"), visibleVideos)

        val unavailable = assertThrows(ServerResponseException::class.java) {
            runBlocking { load() }
        }
        assertEquals(530, unavailable.statusCode)
        assertEquals("subscription_feed_upstream_unavailable", unavailable.failureCode)
        assertEquals("request-530", unavailable.requestId)
        assertEquals(listOf("cached"), visibleVideos)

        visibleVideos = load().videos.map { it.id }
        assertEquals(listOf("fresh"), visibleVideos)
    }

    private suspend fun load() = SubscriptionFeedClient(pause = {}).load(
        api = api,
        cursor = null,
        limit = 12,
        expectedGeneration = null,
        verifyOwner = {},
    )

    private fun readyResponse(id: String, generation: Long) = jsonResponse(
        200,
        """{"videos":[${video(id)}],"nextpage":null,"generation":$generation,"generatedAt":1234,"refreshing":false}""",
    )

    private fun video(id: String) = """
        {
          "id":"$id","title":"$id","url":"https://video.example/$id","thumbnailUrl":"thumb",
          "uploaderName":"Channel","uploaderUrl":"channel","uploaderAvatarUrl":"avatar",
          "duration":42,"viewCount":7,"uploadDate":"","uploaded":1,
          "streamType":"VIDEO_STREAM","isShortFormContent":false,"uploaderVerified":false
        }
    """.trimIndent()

    private fun jsonResponse(status: Int, body: String) = MockResponse()
        .setResponseCode(status)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
