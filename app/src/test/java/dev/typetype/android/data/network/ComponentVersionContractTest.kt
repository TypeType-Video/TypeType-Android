package dev.typetype.android.data.network

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ComponentVersionContractTest {
    private lateinit var server: MockWebServer
    private lateinit var api: TypeTypeApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = RetrofitFactory(
            sessionClient = OkHttpClient(),
            json = Json { ignoreUnknownKeys = true },
        ).create(server.url("/api/").toString())
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `uses every component version endpoint exposed by the frontend gateway`() = runBlocking {
        val requests = listOf(
            "/api/version/web" to api::frontendVersion,
            "/api/version/server" to api::serverVersion,
            "/api/version/token" to api::tokenVersion,
            "/api/version/downloader" to api::downloaderVersion,
        )

        requests.forEach { (path, request) ->
            server.enqueue(versionResponse())
            val version = requireNotNull(request().body())

            assertEquals(path, server.takeRequest().path)
            assertEquals("1.3.1", version.version)
            assertEquals("revision", version.revision)
        }
    }

    private fun versionResponse() = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(
            """{"service":"server","version":"1.3.1","revision":"revision","shortRevision":"rev","buildTime":"2026-08-05T10:54:40Z"}""",
        )
}
