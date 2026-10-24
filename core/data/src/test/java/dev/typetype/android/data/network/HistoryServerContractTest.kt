package dev.typetype.android.data.network

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HistoryServerContractTest {
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
    fun filtersUseServerSearchAndDateContract() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setHeader("X-Total-Count", "0")
                .setBody("[]"),
        )

        api.history(
            search = "video title",
            fromMillis = 1_500L,
            toMillis = 3_000L,
            limit = 500,
            offset = 0,
        )

        assertEquals(
            "/history?q=video%20title&from=1500&to=3000&limit=500&offset=0",
            server.takeRequest().path,
        )
    }
}
