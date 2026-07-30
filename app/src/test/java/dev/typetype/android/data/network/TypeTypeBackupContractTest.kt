package dev.typetype.android.data.network

import java.util.concurrent.TimeUnit
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TypeTypeBackupContractTest {
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
    fun exportSendsSelectedCategoriesAndReturnsBackupBytes() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"format":"typetype-backup","version":1}"""),
        )

        val response = api.exportTypeType("subscriptions,watchLater,contentFilters")

        assertEquals(
            "/backup/typetype?categories=subscriptions%2CwatchLater%2CcontentFilters",
            server.takeRequest().path,
        )
        assertEquals("""{"format":"typetype-backup","version":1}""", response.body()?.string())
    }

    @Test
    fun restoreUploadsOneJsonFileAndDecodesCounts() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"restored":{"subscriptions":2,"history":5}}"""),
        )
        val part = MultipartBody.Part.createFormData(
            "file",
            "typetype-backup.json",
            """{"format":"typetype-backup","version":1}"""
                .toRequestBody("application/json".toMediaType()),
        )

        val response = api.restoreTypeType(part)
        val request = server.takeRequest(1, TimeUnit.SECONDS)

        assertNotNull(request)
        assertEquals("POST", request?.method)
        assertEquals("/restore/typetype", request?.path)
        assertTrue(request?.headers?.get("Content-Type").orEmpty().startsWith("multipart/form-data"))
        assertTrue(request?.body?.readUtf8().orEmpty().contains("typetype-backup.json"))
        assertEquals(2, response.body()?.restored?.get("subscriptions"))
        assertEquals(5, response.body()?.restored?.get("history"))
    }
}
