package dev.typetype.android.data.feed

import dev.typetype.android.data.network.RetrofitFactory
import dev.typetype.android.data.network.TypeTypeApi
import dev.typetype.android.domain.feed.ShortsContinuation
import dev.typetype.android.domain.feed.Video
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ShortsFeedPagesTest {
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
    fun recommendationsUseTheDedicatedEndpointAndOpaqueCursor() = runBlocking {
        server.enqueue(pageResponse("items", nextField = "\"next/token\"", hasMore = true))

        val page = loadRecommendationShorts(api, "current/token", service = 2, limit = 24)

        val request = server.takeRequest().requestUrl
        assertEquals("/recommendations/shorts", request?.encodedPath)
        assertEquals("2", request?.queryParameter("service"))
        assertEquals("24", request?.queryParameter("limit"))
        assertEquals("auto", request?.queryParameter("intent"))
        assertEquals("current/token", request?.queryParameter("cursor"))
        assertEquals(ShortsContinuation.Recommendations("next/token"), page.continuation)
    }

    @Test
    fun subscriptionFallbackUsesNumericPagesAndBlending() = runBlocking {
        server.enqueue(pageResponse("videos", nextField = "\"3\""))

        val page = loadSubscriptionShorts(api, page = 2, service = 1, limit = 30)

        val request = server.takeRequest().requestUrl
        assertEquals("/subscriptions/shorts", request?.encodedPath)
        assertEquals("2", request?.queryParameter("page"))
        assertEquals("true", request?.queryParameter("blended"))
        assertEquals(ShortsContinuation.Subscriptions(3), page.continuation)
    }

    @Test
    fun guestDiscoveryUsesTheStandardSearchContract() = runBlocking {
        server.enqueue(searchResponse(nextPage = "opaque-next"))

        val page = loadDiscoveryShorts(api, nextPage = null, service = 0, limit = 1)

        val request = server.takeRequest().requestUrl
        assertEquals("/search", request?.encodedPath)
        assertEquals("shorts", request?.queryParameter("q"))
        assertEquals(1, page.videos.size)
        assertEquals(ShortsContinuation.Discovery("opaque-next"), page.continuation)
    }

    @Test
    fun normalizationFiltersDeduplicatesAndInterleavesChannels() {
        val videos = listOf(
            video("a1", "channel-a", 15),
            video("a2", "channel-a", 30),
            video("b1", "channel-b", 45),
            video("long", "channel-c", 181),
            video("a1", "channel-a", 15),
        )

        val normalized = videos.normalizedShorts()

        assertEquals(listOf("a1", "b1", "a2"), normalized.map(Video::id))
    }

    @Test
    fun exhaustedSubscriptionPageHasNoContinuation() = runBlocking {
        server.enqueue(pageResponse("videos", nextField = "null"))

        val page = loadSubscriptionShorts(api, page = 0, service = 0, limit = 30)

        assertNull(page.continuation)
    }

    private fun pageResponse(
        collection: String,
        nextField: String,
        hasMore: Boolean = false,
    ) = jsonResponse(
        """{"$collection":[$VIDEO],"nextCursor":$nextField,"nextpage":$nextField,"hasMore":$hasMore}""",
    )

    private fun searchResponse(nextPage: String) = jsonResponse(
        """{"items":[$VIDEO,$VIDEO],"channels":[],"playlists":[],"nextpage":"$nextPage"}""",
    )

    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)

    private fun video(id: String, channel: String, duration: Long) = Video(
        id = id,
        url = "https://video/$id",
        title = id,
        thumbnailUrl = "thumb",
        uploaderName = channel,
        uploaderUrl = channel,
        uploaderAvatarUrl = "avatar",
        uploaderVerified = false,
        durationSeconds = duration,
        isLive = false,
        viewCount = 1,
        uploadedAtMillis = 1,
        isShortFormContent = false,
        shortDescription = null,
    )

    private companion object {
        val VIDEO = """
            {
              "id":"short","title":"Short","url":"https://video/short",
              "thumbnailUrl":"thumb","uploaderName":"Channel",
              "uploaderUrl":"channel","uploaderAvatarUrl":"avatar",
              "duration":42,"viewCount":7,"uploadDate":"","uploaded":1,
              "streamType":"VIDEO_STREAM","isShortFormContent":true,
              "uploaderVerified":false
            }
        """.trimIndent()
    }
}
