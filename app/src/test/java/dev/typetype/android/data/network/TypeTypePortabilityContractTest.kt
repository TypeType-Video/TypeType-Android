package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.PortabilityApplyRequestDto
import dev.typetype.android.data.network.dto.PortabilityExportRequestDto
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class TypeTypePortabilityContractTest {
    private lateinit var server: MockWebServer
    private lateinit var api: TypeTypeImportApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(
                Json { ignoreUnknownKeys = true; explicitNulls = false }
                    .asConverterFactory("application/json".toMediaType()),
            )
            .build()
            .create(TypeTypeImportApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun portabilityFormatsDecodeJobCapabilities() = runBlockingCompat {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                [{
                  "format":"typetype",
                  "adapterVersion":2,
                  "defaultExtension":"json",
                  "contentType":"application/json",
                  "capabilities":[
                    {"category":"subscriptions","directions":["import","export"],"fidelity":"complete"},
                    {"category":"history","directions":["export"],"fidelity":"partial"}
                  ]
                }]
                """.trimIndent(),
            ),
        )

        val formats = api.portabilityFormats().body().orEmpty()
        assertEquals("typetype", formats.single().format)
        assertEquals("/portability/formats", server.takeRequest().path)
    }

    @Test
    fun portabilityExportAndApplyUseExpectedContract() = runBlockingCompat {
        val body = """
            {"id":"job-1","kind":"export","state":"queued","createdAt":1,"updatedAt":2,
             "progress":{"phase":"collecting","unit":"records","processed":2,"total":10}}
        """.trimIndent()
        repeat(2) { server.enqueue(MockResponse().setResponseCode(200).setBody(body)) }

        val export = api.startPortabilityExport(
            request = PortabilityExportRequestDto("typetype", listOf("subscriptions")),
        ).body() ?: error("Missing export job")
        val applied = api.applyPortabilityImport(
            jobId = export.id,
            request = PortabilityApplyRequestDto(listOf("history"), "skip"),
        ).body() ?: error("Missing applied job")

        assertEquals("/portability/exports", server.takeRequest().path)
        assertEquals("/portability/jobs/job-1/apply", server.takeRequest().path)
        assertEquals("queued", export.state)
        assertEquals("queued", applied.state)
    }
}

private fun <T> runBlockingCompat(block: suspend () -> T): T =
    kotlinx.coroutines.runBlocking { block() }
