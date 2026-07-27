package dev.typetype.android.data.network

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SabrControlClientTest {
    @Test
    fun serverPreparationKeepsTheBoundedApplicationTimeout() {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .build()

        val sabr = client.sabrControlClient()

        assertEquals(15_000, sabr.connectTimeoutMillis)
        assertEquals(30_000, sabr.readTimeoutMillis)
        assertEquals(15_000, sabr.writeTimeoutMillis)
        assertEquals(45_000, sabr.callTimeoutMillis)
        assertFalse(sabr.followRedirects)
        assertFalse(sabr.followSslRedirects)
        assertFalse(sabr.retryOnConnectionFailure)
    }

    @Test
    fun serverRetryHintDoesNotTriggerAnUncountedOkHttpRetry() {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(
                MockResponse()
                    .setResponseCode(503)
                    .setHeader("Retry-After", "0"),
            )
            server.enqueue(MockResponse().setResponseCode(200))
            val client = OkHttpClient().sabrControlClient()
            val request = Request.Builder().url(server.url("/playback")).build()

            client.newCall(request).execute().use { response ->
                assertEquals(503, response.code)
            }
            assertEquals(1, server.requestCount)
        } finally {
            server.shutdown()
        }
    }
}
