package dev.typetype.android.data.network

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SabrControlClientTest {
    @Test
    fun slowServerPreparationHasASeparateBoundedTimeout() {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .build()

        val sabr = client.sabrControlClient()

        assertEquals(15_000, sabr.connectTimeoutMillis)
        assertEquals(120_000, sabr.readTimeoutMillis)
        assertEquals(15_000, sabr.writeTimeoutMillis)
        assertEquals(150_000, sabr.callTimeoutMillis)
        assertFalse(sabr.followRedirects)
        assertFalse(sabr.followSslRedirects)
    }
}
