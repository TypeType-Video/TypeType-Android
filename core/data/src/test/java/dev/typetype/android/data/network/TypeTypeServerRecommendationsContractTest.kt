package dev.typetype.android.data.network

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

class TypeTypeServerRecommendationsContractTest {
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
    fun homeRecommendationsUseTheStableServerRouteAndQuery() = runBlocking {
        server.enqueue(response("""{"items":[],"hasMore":false}"""))

        val result = api.homeRecommendations(
            service = 0,
            limit = 12,
            intent = "auto",
            cursor = "opaque",
        )

        assertTrue(result.isSuccessful)
        assertEquals(
            "/recommendations/home?service=0&limit=12&intent=auto&cursor=opaque",
            server.takeRequest().path,
        )
    }

    @Test
    fun shortsRecommendationsUseTheRuntimeServerRouteAndQuery() = runBlocking {
        server.enqueue(response("""{"items":[],"hasMore":false}"""))

        val result = api.shortsRecommendations(
            service = 4,
            limit = 5,
            intent = "quick",
            cursor = "opaque",
        )

        assertTrue(result.isSuccessful)
        assertEquals(
            "/recommendations/shorts?service=4&limit=5&intent=quick&cursor=opaque",
            server.takeRequest().path,
        )
    }

    @Test
    fun recommenderSettingsUseStableGetAndReplaceRoutes() = runBlocking {
        server.enqueue(
            response("""{"enabled":false,"algorithmId":"signals","customInterests":["retro computers"]}"""),
        )
        server.enqueue(
            response("""{"enabled":true,"algorithmId":"signals","customInterests":["retro computers"]}"""),
        )

        val existing = requireNotNull(api.recommenderSettings().body())
        val saved = api.updateRecommenderSettings(
            buildJsonObject {
                put("enabled", true)
                put("algorithmId", "signals")
                put("customInterests", existing.getValue("customInterests"))
            },
        )

        assertTrue(saved.isSuccessful)
        assertEquals(
            "/recommendations/settings",
            server.takeRequest().path,
        )
        val updateRequest = server.takeRequest()
        assertEquals("PUT", updateRequest.method)
        assertEquals("/recommendations/settings", updateRequest.path)
        assertEquals(
            """{"enabled":true,"algorithmId":"signals","customInterests":["retro computers"]}""",
            updateRequest.body.readUtf8(),
        )
    }

    private fun response(body: String): MockResponse = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
