package dev.typetype.android.data.network

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class YoutubeTakeoutImportClientTest {
    @Test
    fun longImportUsesBoundedTimeoutsWithoutImplicitPostRetry() {
        val base = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .build()

        val client = base.youtubeTakeoutImportClient()

        assertEquals(15_000, client.connectTimeoutMillis)
        assertEquals(120_000, client.writeTimeoutMillis)
        assertEquals(900_000, client.readTimeoutMillis)
        assertEquals(21_600_000, client.callTimeoutMillis)
        assertFalse(client.followRedirects)
        assertFalse(client.followSslRedirects)
        assertFalse(client.retryOnConnectionFailure)
    }
}
