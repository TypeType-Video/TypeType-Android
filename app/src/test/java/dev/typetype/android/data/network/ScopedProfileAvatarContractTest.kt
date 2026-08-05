package dev.typetype.android.data.network

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScopedProfileAvatarContractTest {
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
    fun uploadsRawAvatarWithTheActiveAccountToken() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"avatarUrl":"/avatar/custom/user/1","mediaType":"image/gif","size":4}""",
                ),
        )
        val baseUrl = server.url("/api/").toString()
        val scope = NetworkRequestScope("server", "account", baseUrl)
        val client = OkHttpClient.Builder()
            .addInterceptor(ScopedRequestInterceptor(scope) { "avatar-token" })
            .build()
        val api = RetrofitFactory(
            sessionClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true },
        ).createWithClient(baseUrl, TypeTypeApi::class.java, client)
        val bytes = byteArrayOf(0x47, 0x49, 0x46, 0x38)

        val response = api.uploadCustomAvatar(
            bytes.toRequestBody("image/gif".toMediaType()),
        )

        assertTrue(response.isSuccessful)
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/profile/avatar/custom", request.path)
        assertEquals("Bearer avatar-token", request.getHeader("Authorization"))
        assertEquals("image/gif", request.getHeader("Content-Type"))
        assertArrayEquals(bytes, request.body.readByteArray())
    }
}
