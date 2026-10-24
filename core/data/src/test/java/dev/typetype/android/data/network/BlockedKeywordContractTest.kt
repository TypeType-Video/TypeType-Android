package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.BlockedKeywordRequest
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

class BlockedKeywordContractTest {
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
    fun blockedKeywordsDecodeServerMetadata() = runBlocking {
        server.enqueue(jsonResponse("""[{"keyword":"spoiler","blockedAt":42,"global":true}]"""))

        val response = api.blockedKeywords()

        assertEquals("/blocked/keywords", server.takeRequest().path)
        val item = response.body()?.single()
        assertEquals("spoiler", item?.keyword)
        assertEquals(42L, item?.blockedAt)
        assertEquals(true, item?.global)
    }

    @Test
    fun addingKeywordUsesTypedJsonBody() = runBlocking {
        server.enqueue(jsonResponse("""{"keyword":"spoiler","blockedAt":42,"global":false}""", 201))

        api.blockKeyword(BlockedKeywordRequest("Spoiler"))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/blocked/keywords", request.path)
        assertTrue(request.body.readUtf8().contains("\"keyword\":\"Spoiler\""))
    }

    @Test
    fun removingKeywordEncodesItAsOnePathSegment() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))

        api.unblockKeyword("plot twist/ending")

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/blocked/keywords/plot%20twist%2Fending", request.requestUrl?.encodedPath)
    }

    private fun jsonResponse(body: String, status: Int = 200) = MockResponse()
        .setResponseCode(status)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
