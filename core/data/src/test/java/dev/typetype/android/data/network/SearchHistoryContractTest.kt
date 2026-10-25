package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.SearchHistoryEntryRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SearchHistoryContractTest {
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
    fun historyDecodesServerItems() = runBlocking {
        server.enqueue(jsonResponse("""[
            {"id":"entry-1","term":"compose","searchedAt":123}
        ]"""))

        val response = api.searchHistory()

        val request = server.takeRequest()
        assertEquals("/search-history", request.requestUrl?.encodedPath)
        assertEquals("entry-1", response.body()?.single()?.id)
        assertEquals("compose", response.body()?.single()?.term)
        assertEquals(123L, response.body()?.single()?.searchedAt)
    }

    @Test
    fun historyEntryUsesTheServerTermField() = runBlocking {
        server.enqueue(jsonResponse("""{
            "id":"entry-1","term":"compose","searchedAt":123
        }""").setResponseCode(201))

        val response = api.addSearchHistory(SearchHistoryEntryRequest(term = "compose"))

        val body = server.takeRequest().body.readUtf8()
        assertEquals("entry-1", response.body()?.id)
        assertTrue(body.contains("\"term\":\"compose\""))
        assertFalse(body.contains("\"query\""))
    }

    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
