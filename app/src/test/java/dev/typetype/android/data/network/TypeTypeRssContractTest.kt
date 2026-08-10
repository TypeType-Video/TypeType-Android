package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.RssFeedEnabledRequestDto
import dev.typetype.android.data.network.dto.RssFeedRequestDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class TypeTypeRssContractTest {
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
    fun managesTheCompletePrivateFeedLifecycle() = runBlocking {
        server.enqueue(jsonResponse("[$FEED]"))
        server.enqueue(jsonResponse(SECRET, 201))
        server.enqueue(jsonResponse(FEED))
        server.enqueue(jsonResponse(DISABLED_FEED))
        server.enqueue(jsonResponse(SECRET))
        server.enqueue(MockResponse().setResponseCode(204))

        assertEquals(1, api.rssFeeds().body()?.size)
        api.createRssFeed(REQUEST).body()
        api.updateRssFeed("feed-1", REQUEST).body()
        assertFalse(requireNotNull(api.setRssFeedEnabled("feed-1", ENABLED).body()).enabled)
        assertEquals("https://example.test/rss/private", api.regenerateRssFeed("feed-1").body()?.feedUrl)
        assertEquals(204, api.deleteRssFeed("feed-1").code())

        assertEquals("GET", server.takeRequest().method)
        val create = server.takeRequest()
        assertEquals("POST", create.method)
        assertEquals("/rss/feeds", create.path)
        assertEquals(REQUEST_JSON, create.body.readUtf8())
        val update = server.takeRequest()
        assertEquals("PUT", update.method)
        assertEquals("/rss/feeds/feed-1", update.path)
        assertEquals(REQUEST_JSON, update.body.readUtf8())
        val enabled = server.takeRequest()
        assertEquals("/rss/feeds/feed-1/enabled", enabled.path)
        assertEquals("{\"enabled\":false}", enabled.body.readUtf8())
        assertEquals("/rss/feeds/feed-1/regenerate", server.takeRequest().path)
        assertEquals("/rss/feeds/feed-1", server.takeRequest().path)
    }

    @Test
    fun preservesTypedRssErrorsAndRequestIds() = runBlocking {
        listOf(
            403 to "rss_disabled",
            404 to "rss_feed_not_found",
            409 to "rss_feed_limit_reached",
            429 to "rss_rate_limited",
        ).forEach { (status, code) ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(status)
                    .setHeader("Content-Type", "application/json")
                    .setHeader("X-Request-ID", "rss-$status")
                    .setBody("{\"code\":\"$code\",\"error\":\"$code\"}"),
            )
            val response = api.rssFeeds()
            val failure = runCatching { response.requireSuccessfulResponse() }.exceptionOrNull()
                as ServerResponseException
            assertEquals(code, failure.failureCode)
            assertEquals(status, failure.statusCode)
            assertEquals("rss-$status", failure.requestId)
        }
    }

    private fun jsonResponse(body: String, code: Int = 200) = MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "application/json")
        .setBody(body)

    private companion object {
        val REQUEST = RssFeedRequestDto(
            name = "My feed",
            scope = "channels",
            channelUrls = listOf("https://youtube.com/@example"),
            serviceIds = listOf(0, 6),
            includeVideos = true,
            includeShorts = false,
            includeLive = true,
            includeUpcoming = false,
        )
        val ENABLED = RssFeedEnabledRequestDto(enabled = false)
        const val REQUEST_JSON =
            """{"name":"My feed","scope":"channels","channelUrls":["https://youtube.com/@example"],"serviceIds":[0,6],"includeVideos":true,"includeShorts":false,"includeLive":true,"includeUpcoming":false}"""
        const val FEED =
            """{"id":"feed-1","name":"My feed","scope":"channels","channelUrls":["https://youtube.com/@example"],"serviceIds":[0,6],"includeVideos":true,"includeShorts":false,"includeLive":true,"includeUpcoming":false,"enabled":true,"createdAt":1,"updatedAt":2,"lastUsedAt":null}"""
        const val DISABLED_FEED =
            """{"id":"feed-1","name":"My feed","scope":"channels","channelUrls":["https://youtube.com/@example"],"serviceIds":[0,6],"includeVideos":true,"includeShorts":false,"includeLive":true,"includeUpcoming":false,"enabled":false,"createdAt":1,"updatedAt":2,"lastUsedAt":null}"""
        const val SECRET = """{"feed":$FEED,"feedUrl":"https://example.test/rss/private"}"""
    }
}
