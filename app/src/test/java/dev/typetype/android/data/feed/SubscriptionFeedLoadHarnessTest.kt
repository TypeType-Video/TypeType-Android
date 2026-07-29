package dev.typetype.android.data.feed

import dev.typetype.android.data.network.RetrofitFactory
import dev.typetype.android.data.network.TypeTypeFeedApi
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SubscriptionFeedLoadHarnessTest {
    private lateinit var server: MockWebServer
    private lateinit var api: TypeTypeFeedApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = FeedDispatcher()
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
    fun traversesALargeSnapshotWithoutDuplicatesOrRemoteInfrastructure() = runBlocking {
        val videos = mutableListOf<String>()
        val client = SubscriptionFeedClient(pause = {})
        var cursor: String? = null
        var generation: Long? = null
        var pages = 0

        val elapsedMillis = measureTimeMillis {
            do {
                val page = client.load(
                    api = api,
                    cursor = cursor,
                    limit = PAGE_SIZE,
                    expectedGeneration = generation,
                    verifyOwner = {},
                )
                generation = page.generation
                cursor = page.nextCursor
                videos += page.videos.map { it.id }
                pages += 1
            } while (cursor != null)
        }

        assertEquals(TOTAL_VIDEOS, videos.size)
        assertEquals(TOTAL_VIDEOS, videos.distinct().size)
        assertEquals(EXPECTED_PAGES, pages)
        assertEquals(EXPECTED_PAGES + PREPARING_RESPONSES, server.requestCount)
        assertTrue("Local snapshot traversal took ${elapsedMillis}ms", elapsedMillis < 10_000L)
        println(
            "subscription_load_harness videos=$TOTAL_VIDEOS pages=$pages " +
                "requests=${server.requestCount} elapsedMs=$elapsedMillis",
        )
    }

    private class FeedDispatcher : Dispatcher() {
        private val requests = AtomicInteger()

        override fun dispatch(request: RecordedRequest): MockResponse {
            val requestNumber = requests.incrementAndGet()
            if (requestNumber <= PREPARING_RESPONSES) {
                return jsonResponse(
                    202,
                    """{"code":"subscription_feed_preparing","retryAfterMs":100}""",
                )
            }
            val cursor = request.requestUrl
                ?.queryParameter("cursor")
                ?.removePrefix(CURSOR_PREFIX)
                ?.toIntOrNull()
                ?: 0
            val start = cursor * PAGE_SIZE
            val count = min(PAGE_SIZE, TOTAL_VIDEOS - start)
            val nextCursor = (cursor + 1)
                .takeIf { start + count < TOTAL_VIDEOS }
                ?.let { "\"$CURSOR_PREFIX$it\"" }
                ?: "null"
            val videos = (start until start + count).joinToString(",") { videoJson(it) }
            return jsonResponse(
                200,
                """{"videos":[$videos],"nextpage":$nextCursor,"generation":$GENERATION,"generatedAt":1234,"refreshing":false}""",
            )
        }
    }

    private companion object {
        const val TOTAL_VIDEOS = 4_200
        const val PAGE_SIZE = 100
        const val EXPECTED_PAGES = 42
        const val PREPARING_RESPONSES = 2
        const val GENERATION = 42L
        const val CURSOR_PREFIX = "opaque:"

        fun videoJson(index: Int) = """
            {
              "id":"video-$index","title":"Video $index",
              "url":"https://video.example/watch?v=$index",
              "thumbnailUrl":"https://image.example/$index.jpg",
              "uploaderName":"Channel ${index % 50}",
              "uploaderUrl":"https://video.example/channel/${index % 50}",
              "uploaderAvatarUrl":"https://image.example/avatar/${index % 50}.jpg",
              "duration":${60 + index},"viewCount":${index * 10L},
              "uploadDate":"","uploaded":${1_000_000L + index},
              "streamType":"VIDEO_STREAM","isShortFormContent":false,
              "uploaderVerified":${index % 2 == 0}
            }
        """.trimIndent()

        fun jsonResponse(status: Int, body: String) = MockResponse()
            .setResponseCode(status)
            .setHeader("Content-Type", "application/json")
            .setBody(body)
    }
}
