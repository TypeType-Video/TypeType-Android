package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.RegisterRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TypeTypeRegistrationContractTest {
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
    fun registrationStatusMatchesTheServerContract() = runBlocking {
        server.enqueue(jsonResponse("""{"allowRegistration":false,"bootstrapAvailable":true,"localLoginEnabled":true}"""))

        val status = requireNotNull(api.registerStatus().body())

        assertFalse(status.allowRegistration)
        assertTrue(status.bootstrapAvailable)
        assertTrue(status.localLoginEnabled)
        assertEquals("/auth/register/status", server.takeRequest().path)
    }

    @Test
    fun registrationSendsIdentityAndReturnsTheSession() = runBlocking {
        server.enqueue(jsonResponse("""{"accessToken":"new-session"}"""))

        val response = api.register(
            RegisterRequest(
                email = "new@example.com",
                password = "secret",
                name = "New user",
            ),
        )

        assertEquals("new-session", response.body()?.accessToken)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/auth/register", request.path)
        assertEquals(
            """{"email":"new@example.com","password":"secret","name":"New user"}""",
            request.body.readUtf8(),
        )
    }

    private fun jsonResponse(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
