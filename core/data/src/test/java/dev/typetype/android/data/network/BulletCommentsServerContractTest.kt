package dev.typetype.android.data.network

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BulletCommentsServerContractTest {
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
    fun bulletCommentsUseTheServerOwnedNicoNicoContract() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"comments":[{"text":"Hello","argbColor":-1,"position":"REGULAR","relativeFontSize":1.0,"durationMs":4200,"isLive":false}],"nextpage":null}""",
                ),
        )

        val response = requireNotNull(api.bulletComments(NICONICO_URL).body())

        assertEquals("Hello", response.comments.single().text)
        assertEquals(
            "/bullet-comments?url=${encode(NICONICO_URL)}",
            server.takeRequest().path,
        )
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")

    private companion object {
        const val NICONICO_URL = "https://www.nicovideo.jp/watch/sm9"
    }
}
