package dev.typetype.android.data.network

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TypeTypeImportContractTest {
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
    fun pipePipeRestoreUsesNormalizedMultipartContract() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"history":4,"subscriptions":3,"playlists":2,"playlistVideos":1,"progress":5,"searchHistory":6,"timeMode":"normalized","historyMinWatchedAt":10,"historyMaxWatchedAt":20}""",
                ),
        )
        val body = "zip-content".toRequestBody("application/zip".toMediaType())
        val file = MultipartBody.Part.createFormData("file", "backup.zip", body)

        val summary = requireNotNull(api.restorePipePipe(file = file).body())

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/restore/pipepipe?timeMode=normalized", request.path)
        assertTrue(request.headers["Content-Type"].orEmpty().startsWith("multipart/form-data"))
        val requestBody = request.body.readUtf8()
        assertTrue(requestBody.contains("name=\"file\"; filename=\"backup.zip\""))
        assertTrue(requestBody.contains("zip-content"))
        assertEquals(4, summary.history)
        assertEquals(6, summary.searchHistory)
    }
}
