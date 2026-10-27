package dev.typetype.android.data.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ScopedRequestInterceptorTest {
    private lateinit var origin: MockWebServer
    private lateinit var otherOrigin: MockWebServer

    @Before
    fun setUp() {
        origin = MockWebServer()
        otherOrigin = MockWebServer()
        origin.start()
        otherOrigin.start()
    }

    @After
    fun tearDown() {
        origin.shutdown()
        otherOrigin.shutdown()
    }

    @Test
    fun attachesTheAccountTokenAndScope() {
        origin.enqueue(MockResponse().setResponseCode(204))
        val scope = NetworkRequestScope("server", "account", origin.url("/api/").toString())
        val client = OkHttpClient.Builder()
            .addInterceptor(ScopedRequestInterceptor(scope) { "private-token" })
            .addInterceptor { chain ->
                assertEquals(scope, chain.request().tag(NetworkRequestScope::class.java))
                chain.proceed(chain.request())
            }
            .build()

        client.newCall(Request.Builder().url(origin.url("/api/health")).build()).execute().close()

        assertEquals("Bearer private-token", origin.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun preservesTheAccountScopeWithoutInventingAToken() {
        origin.enqueue(MockResponse().setResponseCode(204))
        val scope = NetworkRequestScope("server", "account", origin.url("/api/").toString())
        val client = OkHttpClient.Builder()
            .addInterceptor(ScopedRequestInterceptor(scope) { null })
            .addInterceptor { chain ->
                assertEquals(scope, chain.request().tag(NetworkRequestScope::class.java))
                chain.proceed(chain.request())
            }
            .build()

        client.newCall(
            Request.Builder().url(origin.url("/api/sabr/playback/session/manifest")).build(),
        )
            .execute()
            .close()

        assertNull(origin.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun readsTheCurrentAccountTokenForEveryMediaRequest() {
        repeat(2) { origin.enqueue(MockResponse().setResponseCode(204)) }
        val scope = NetworkRequestScope("server", "account", origin.url("/api/").toString())
        var token = "first-token"
        val client = OkHttpClient.Builder()
            .addInterceptor(ScopedRequestInterceptor(scope) { token })
            .build()
        val request = Request.Builder()
            .url(origin.url("/api/sabr/playback/session/137/segment/2"))
            .build()

        client.newCall(request).execute().close()
        token = "refreshed-token"
        client.newCall(request).execute().close()

        assertEquals("Bearer first-token", origin.takeRequest().getHeader("Authorization"))
        assertEquals("Bearer refreshed-token", origin.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun attachesTheAccountTokenToServerSubtitleRequests() {
        origin.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/vtt; charset=utf-8")
                .setBody("WEBVTT"),
        )
        val scope = NetworkRequestScope("server", "account", origin.url("/api/").toString())
        val client = OkHttpClient.Builder()
            .addInterceptor(ScopedRequestInterceptor(scope) { "subtitle-token" })
            .build()

        client.newCall(
            Request.Builder()
                .url(origin.url("/api/subtitles/track.vtt"))
                .build(),
        ).execute().close()

        assertEquals("Bearer subtitle-token", origin.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun doesNotForwardBearerTokenAcrossOrigins() {
        origin.enqueue(
            MockResponse()
                .setResponseCode(302)
                .setHeader("Location", otherOrigin.url("/health").toString()),
        )
        otherOrigin.enqueue(MockResponse().setResponseCode(204))
        val scope = NetworkRequestScope("server", "account", origin.url("/api/").toString())
        val client = OkHttpClient.Builder()
            .addInterceptor(ScopedRequestInterceptor(scope) { "private-token" })
            .build()

        client.newCall(Request.Builder().url(origin.url("/api/health")).build()).execute().close()

        assertEquals("Bearer private-token", origin.takeRequest().getHeader("Authorization"))
        assertNull(otherOrigin.takeRequest().getHeader("Authorization"))
    }
}
