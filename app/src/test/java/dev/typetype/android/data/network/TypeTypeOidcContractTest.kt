package dev.typetype.android.data.network

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TypeTypeOidcContractTest {
    @Test
    fun oidcStatusUsesThePublicServerContract() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        """{"enabled":true,"providerName":"Keycloak","localLoginEnabled":false,"autoRedirect":true}""",
                    ),
            )
            val api = RetrofitFactory(
                sessionClient = OkHttpClient(),
                json = Json { ignoreUnknownKeys = true },
            ).create(server.url("/").toString())

            val status = requireNotNull(api.oidcStatus().body())

            assertTrue(status.enabled)
            assertEquals("Keycloak", status.providerName)
            assertFalse(status.localLoginEnabled)
            assertTrue(status.autoRedirect)
            assertEquals("/auth/oidc/status", server.takeRequest().path)
        } finally {
            server.shutdown()
        }
    }
}
