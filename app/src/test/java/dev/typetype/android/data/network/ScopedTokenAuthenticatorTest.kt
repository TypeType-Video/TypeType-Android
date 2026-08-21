package dev.typetype.android.data.network

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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
        val client = client(scope, tokens)

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

    @Test
    fun subscriptionFeedRequestRecoversAfterTokenRefresh() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"accessToken":"fresh-token"}"""),
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"videos":[],"nextpage":null,"generation":1,"generatedAt":1,"refreshing":false}""",
                ),
        )
        val scope = NetworkRequestScope("server", "account-a", server.url("/api/").toString())
        val tokens = FakeScopedTokenStore("expired-token")

        client(scope, tokens).newCall(
            Request.Builder().url(server.url("/api/subscriptions/feed?limit=30")).build(),
        ).execute().use { response -> assertEquals(200, response.code) }

        assertEquals("/api/subscriptions/feed?limit=30", server.takeRequest().path)
        assertEquals("/api/auth/refresh", server.takeRequest().path)
        val retriedFeed = server.takeRequest()
        assertEquals("/api/subscriptions/feed?limit=30", retriedFeed.path)
        assertEquals("Bearer fresh-token", retriedFeed.getHeader("Authorization"))
    }

    @Test
    fun transientRefreshFailurePreservesTheSavedSession() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(
            MockResponse()
                .setResponseCode(503)
                .setHeader("Content-Type", "application/json")
                .setHeader("X-Request-ID", "refresh-request")
                .setBody("""{"code":"auth_refresh_unavailable"}"""),
        )
        val scope = NetworkRequestScope("server", "account-a", server.url("/api/").toString())
        val tokens = FakeScopedTokenStore("saved-token")

        val failure = assertThrows(SessionRefreshUnavailableException::class.java) {
            client(scope, tokens).newCall(
                Request.Builder().url(server.url("/api/health")).build(),
            ).execute().close()
        }

        assertEquals(503, failure.statusCode)
        assertEquals("auth_refresh_unavailable", failure.failureCode)
        assertEquals("refresh-request", failure.requestId)
        assertEquals("saved-token", tokens.token)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun rejectedRefreshReturnsUnauthorizedWithoutRemovingTheAccount() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))
        val scope = NetworkRequestScope("server", "account-a", server.url("/api/").toString())
        val tokens = FakeScopedTokenStore("expired-token")

        client(scope, tokens).newCall(
            Request.Builder().url(server.url("/api/health")).build(),
        ).execute().use { response -> assertEquals(401, response.code) }

        assertEquals("expired-token", tokens.token)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun invalidSuccessfulRefreshPayloadDoesNotInvalidateTheSession() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val scope = NetworkRequestScope("server", "account-a", server.url("/api/").toString())
        val tokens = FakeScopedTokenStore("saved-token")

        val failure = assertThrows(SessionRefreshUnavailableException::class.java) {
            client(scope, tokens).newCall(
                Request.Builder().url(server.url("/api/health")).build(),
            ).execute().close()
        }

        assertEquals("auth_refresh_payload_invalid", failure.failureCode)
        assertEquals("saved-token", tokens.token)
        assertTrue(failure.cause != null)
    }

    @Test
    fun emptySuccessfulRefreshTokenDoesNotInvalidateTheSession() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"accessToken":""}"""))
        val scope = NetworkRequestScope("server", "account-a", server.url("/api/").toString())
        val tokens = FakeScopedTokenStore("saved-token")

        val failure = assertThrows(SessionRefreshUnavailableException::class.java) {
            client(scope, tokens).newCall(
                Request.Builder().url(server.url("/api/health")).build(),
            ).execute().close()
        }

        assertEquals("auth_refresh_payload_invalid", failure.failureCode)
        assertEquals("saved-token", tokens.token)
        assertTrue(failure.cause != null)
    }

    private fun client(scope: NetworkRequestScope, tokens: FakeScopedTokenStore): OkHttpClient =
        OkHttpClient.Builder()
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
}

private class FakeScopedTokenStore(initialToken: String) : ScopedAccessTokenStore {
    var token: String? = initialToken

    override fun getAccessToken(serverId: String, accountId: String): String? = token

    override fun setAccessToken(serverId: String, accountId: String, token: String?) {
        this.token = token
    }
}
