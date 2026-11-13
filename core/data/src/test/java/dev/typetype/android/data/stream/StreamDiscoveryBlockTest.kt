package dev.typetype.android.data.stream

import dev.typetype.android.data.network.RetrofitFactory
import dev.typetype.android.data.network.TypeTypeApi
import dev.typetype.android.data.network.sabrControlClient
import dev.typetype.android.data.network.serverResponseException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class StreamDiscoveryBlockTest {
    private lateinit var server: MockWebServer
    private lateinit var api: TypeTypeApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = RetrofitFactory(
            sessionClient = OkHttpClient().sabrControlClient(),
            json = Json { ignoreUnknownKeys = true },
        ).create(server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `a provider block reaches the player with its code after one bootstrap and one fallback`() = runBlocking {
        repeat(2) { server.enqueue(blocked()) }

        val response = transientPlaybackRequest({}, serverWait = TransientServerWait.Discovery) {
            api.loadYouTubeSabrBootstrapResponse("https://www.youtube.com/watch?v=eRsGyueVLvQ")
        }
        val failure = serverResponseException(response)

        assertEquals("provider_access_blocked", failure.failureCode)
        assertEquals(503, failure.statusCode)
        assertEquals(2, server.requestCount)
    }

    private fun blocked() = MockResponse()
        .setResponseCode(503)
        .setHeader("Retry-After", "60")
        .setHeader("Content-Type", "application/json")
        .setBody("""{"error":"blocked","code":"provider_access_blocked"}""")
}
