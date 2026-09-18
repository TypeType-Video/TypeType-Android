package dev.typetype.android.data.network

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

class SearchFilterContractTest {
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
    fun filterDiscoveryUsesContentFilterAndDecodesGroups() = runBlocking {
        server.enqueue(jsonResponse(FILTER_RESPONSE))

        val response = api.searchFilters(service = 0, contentFilter = "videos")

        val request = server.takeRequest()
        assertEquals("/search/filters", request.requestUrl?.encodedPath)
        assertEquals("0", request.requestUrl?.queryParameter("service"))
        assertEquals("videos", request.requestUrl?.queryParameter("contentFilter"))
        val group = response.body()?.filterGroups?.single()
        assertEquals("duration", group?.key)
        assertEquals(true, group?.multiSelect)
        assertEquals(true, group?.options?.first()?.isDefault)
    }

    @Test
    fun searchSendsEachGroupedFilterAsRepeatedQueryParameter() = runBlocking {
        server.enqueue(jsonResponse("""{"items":[],"channels":[],"playlists":[]}"""))

        api.search(
            query = "compose",
            contentFilter = "videos",
            filters = listOf("short", "week"),
        )

        val requestUrl = server.takeRequest().requestUrl
        assertEquals(listOf("short", "week"), requestUrl?.queryParameterValues("filter"))
        assertFalse(requestUrl?.queryParameterNames?.contains("sortFilter") == true)
    }

    @Test
    fun searchSendsTheSelectedService() = runBlocking {
        server.enqueue(jsonResponse("""{"items":[],"channels":[],"playlists":[]}"""))

        api.search(query = "test", service = 6)

        assertEquals("6", server.takeRequest().requestUrl?.queryParameter("service"))
    }

    @Test
    fun suggestionsSendTheSelectedService() = runBlocking {
        server.enqueue(jsonResponse("[]"))

        api.searchSuggestions(query = "test", service = 5)

        val requestUrl = server.takeRequest().requestUrl
        assertEquals("test", requestUrl?.queryParameter("query"))
        assertEquals("5", requestUrl?.queryParameter("service"))
    }

    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)

    private companion object {
        const val FILTER_RESPONSE = """{
            "contentFilters":[],
            "sortFilters":[],
            "filterGroups":[{
                "key":"duration",
                "label":"Duration",
                "multiSelect":true,
                "options":[{"value":"all","label":"All","isDefault":true}]
            }]
        }"""
    }
}
