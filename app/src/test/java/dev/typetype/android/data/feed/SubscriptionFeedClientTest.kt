package dev.typetype.android.data.feed

import dev.typetype.android.data.network.RetrofitFactory
import dev.typetype.android.data.network.ServerResponseException
import dev.typetype.android.data.network.TypeTypeFeedApi
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class SubscriptionFeedClientTest {
    private lateinit var server: MockWebServer
    private lateinit var api: TypeTypeFeedApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = RetrofitFactory(
            sessionClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true },
        ).create(server.url("/").toString(), TypeTypeFeedApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun preparationKeepsTheOpaqueCursorAndClampsTheRetryDelay() = runBlocking {
        server.enqueue(jsonResponse(202, """{"code":"subscription_feed_preparing","retryAfterMs":50}"""))
        server.enqueue(readyResponse(generation = 7, nextpage = "next"))
        val delays = mutableListOf<Long>()

        val page = SubscriptionFeedClient(pause = delays::add).load(
            api = api,
            cursor = "opaque/cursor",
            limit = 12,
            expectedGeneration = 7,
            verifyOwner = {},
        )

        assertEquals(listOf(100L), delays)
        assertEquals(7L, page.generation)
        assertEquals("next", page.nextCursor)
        assertFalse(page.refreshing)
        repeat(2) {
            assertEquals(
                "/subscriptions/feed?limit=12&cursor=opaque%2Fcursor",
                server.takeRequest(1, TimeUnit.SECONDS)?.path,
            )
        }
    }

    @Test
    fun staleGenerationPreservesTheTypedServerFailure() {
        server.enqueue(
            jsonResponse(
                409,
                """{"error":"Generation expired","code":"subscription_feed_stale_generation"}""",
            ),
        )

        val failure = assertThrows(ServerResponseException::class.java) {
            runBlocking {
                SubscriptionFeedClient().load(api, "cursor", 12, 9, verifyOwner = {})
            }
        }

        assertEquals(STALE_GENERATION_CODE, failure.failureCode)
        assertEquals(409, failure.statusCode)
    }

    @Test
    fun continuationRejectsAResponseFromAnotherGeneration() {
        server.enqueue(readyResponse(generation = 10, nextpage = null))

        val failure = assertThrows(SubscriptionFeedContractException::class.java) {
            runBlocking {
                SubscriptionFeedClient().load(api, "cursor", 12, 9, verifyOwner = {})
            }
        }

        assertEquals(GENERATION_MISMATCH_CODE, failure.failureCode)
    }

    @Test
    fun cancellationDuringPreparationEscapesTheRepositoryStateMachine() {
        server.enqueue(jsonResponse(202, """{"code":"subscription_feed_preparing","retryAfterMs":500}"""))
        val client = SubscriptionFeedClient(pause = { throw CancellationException("left screen") })

        assertThrows(CancellationException::class.java) {
            runBlocking { client.load(api, null, 12, null, verifyOwner = {}) }
        }
    }

    @Test
    fun exhaustedReadyPageHasNoContinuation() = runBlocking {
        server.enqueue(readyResponse(generation = 11, nextpage = null))

        val page = SubscriptionFeedClient().load(api, null, 12, null, verifyOwner = {})

        assertNull(page.nextCursor)
        assertFalse(page.hasMore)
        assertEquals("Video", page.videos.single().title)
    }

    private fun readyResponse(generation: Long, nextpage: String?): MockResponse {
        val cursor = nextpage?.let { "\"$it\"" } ?: "null"
        return jsonResponse(
            200,
            """{"videos":[$VIDEO],"nextpage":$cursor,"generation":$generation,"generatedAt":1234,"refreshing":false}""",
        )
    }

    private fun jsonResponse(status: Int, body: String): MockResponse = MockResponse()
        .setResponseCode(status)
        .setHeader("Content-Type", "application/json")
        .setBody(body)

    private companion object {
        val VIDEO = """
            {
              "id":"id","title":"Video","url":"video","thumbnailUrl":"thumb",
              "uploaderName":"Channel","uploaderUrl":"channel","uploaderAvatarUrl":"avatar",
              "duration":42,"viewCount":7,"uploadDate":"","uploaded":1,
              "streamType":"VIDEO_STREAM","isShortFormContent":false,"uploaderVerified":false
            }
        """.trimIndent()
    }
}
