package dev.typetype.android.data.network

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ScopedTokenAuthenticatorTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun refreshesAndRetriesOnlyTheScopedAccount() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"accessToken":"fresh-token"}"""),
        )
        server.enqueue(MockResponse().setResponseCode(204))
        val scope = NetworkRequestScope("server", "account-a", server.url("/api/").toString())
        val tokens = FakeScopedTokenStore("expired-token")
        val client = OkHttpClient.Builder()
            .addInterceptor(ScopedRequestInterceptor(scope) { tokens.token })
            .authenticator(
                ScopedTokenAuthenticator(
                    scope = scope,
                    tokenStore = tokens,
                    refreshClient = OkHttpClient(),
                    json = Json,
                    lock = Any(),
                ),
            )
            .build()

        client.newCall(Request.Builder().url(server.url("/api/health")).build()).execute().close()
        server.enqueue(MockResponse().setResponseCode(204))
        client.newCall(Request.Builder().url(server.url("/api/health")).build()).execute().close()

        val initial = server.takeRequest()
        val refresh = server.takeRequest()
        val retry = server.takeRequest()
        val nextRequest = server.takeRequest()
        assertEquals("Bearer expired-token", initial.getHeader("Authorization"))
        assertEquals("/api/auth/refresh", refresh.path)
        assertEquals("Bearer fresh-token", retry.getHeader("Authorization"))
        assertEquals("Bearer fresh-token", nextRequest.getHeader("Authorization"))
        assertEquals("fresh-token", tokens.token)
    }
}

private class FakeScopedTokenStore(initialToken: String) : ScopedAccessTokenStore {
    var token: String? = initialToken

    override fun getAccessToken(serverId: String, accountId: String): String? = token

    override fun setAccessToken(serverId: String, accountId: String, token: String?) {
        this.token = token
    }
}
