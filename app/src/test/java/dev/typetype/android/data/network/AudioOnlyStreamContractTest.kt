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

class AudioOnlyStreamContractTest {
    private lateinit var server: MockWebServer
    private lateinit var api: TypeTypeMediaApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = RetrofitFactory(
            sessionClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true },
        ).create(server.url("/api/").toString(), TypeTypeMediaApi::class.java)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `matches the frontend audio-only request contract`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"src":"/streams/audio-only/source?token=value","kind":"progressive","mimeType":"audio/mp4","codec":"mp4a.40.2","bitrate":128000,"contentLength":4096,"duration":42}""",
                ),
        )

        val body = requireNotNull(api.audioOnlyStream(VIDEO_URL, true, "fr").body())

        assertEquals("progressive", body.kind)
        assertEquals("audio/mp4", body.mimeType)
        assertEquals(
            "/api/streams/audio-only?url=${encode(VIDEO_URL)}&preferOriginal=true&preferredLocale=fr",
            server.takeRequest().path,
        )
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")

    private companion object {
        const val VIDEO_URL = "https://www.youtube.com/watch?v=video"
    }
}
