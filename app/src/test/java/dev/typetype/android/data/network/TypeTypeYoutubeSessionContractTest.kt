package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.YoutubeRemoteBrowserStartRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TypeTypeYoutubeSessionContractTest {
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
    fun statusMatchesTheServerContract() = runBlocking {
        server.enqueue(jsonResponse("""{"status":"needs_reconnect","updatedAt":42,"lastUsedAt":21}"""))

        val status = requireNotNull(api.youtubeSessionStatus().body())

        assertEquals("needs_reconnect", status.status)
        assertEquals(42L, status.updatedAt)
        assertEquals(21L, status.lastUsedAt)
        assertEquals("/youtube-session/status", server.takeRequest().path)
    }

    @Test
    fun browserLifecycleUsesTheDocumentedRoutes() = runBlocking {
        server.enqueue(
            jsonResponse(
                """{"sessionId":"remote-id","wsUrl":"/youtube-session/browser/remote-id?token=one-time","expiresAt":123}""",
                code = 201,
            ),
        )
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(MockResponse().setResponseCode(204))

        val started = requireNotNull(
            api.startYoutubeRemoteBrowser(YoutubeRemoteBrowserStartRequest(returnTo = "/watch?v=video")).body(),
        )
        assertEquals("remote-id", started.sessionId)
        assertEquals(123L, started.expiresAt)
        val startRequest = server.takeRequest()
        assertEquals("POST", startRequest.method)
        assertEquals("/youtube-session/browser/start", startRequest.path)
        assertEquals("""{"returnTo":"/watch?v=video"}""", startRequest.body.readUtf8())

        api.cancelYoutubeRemoteBrowser(started.sessionId)
        val cancelRequest = server.takeRequest()
        assertEquals("DELETE", cancelRequest.method)
        assertEquals("/youtube-session/browser/remote-id", cancelRequest.path)

        api.disconnectYoutubeSession()
        val disconnectRequest = server.takeRequest()
        assertEquals("DELETE", disconnectRequest.method)
        assertEquals("/youtube-session", disconnectRequest.path)
    }

    private fun jsonResponse(body: String, code: Int = 200) = MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
