package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.YoutubeTakeoutCommitRequestDto
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

    @Test
    fun youtubeTakeoutUsesTheServerJobContract() = runBlocking {
        server.enqueue(jsonResponse(JOB_STATUS, 201))
        server.enqueue(jsonResponse(PREVIEW))
        server.enqueue(jsonResponse(JOB_STATUS, 202))
        server.enqueue(jsonResponse(JOB_STATUS))
        server.enqueue(jsonResponse(REPORT))
        val archive = MultipartBody.Part.createFormData(
            "archive",
            "takeout.zip",
            "zip-content".toRequestBody("application/zip".toMediaType()),
        )

        val created = requireNotNull(api.uploadYoutubeTakeout(archive).body())
        val preview = requireNotNull(api.youtubeTakeoutPreview(created.jobId).body())
        api.commitYoutubeTakeout(
            created.jobId,
            YoutubeTakeoutCommitRequestDto(
                importSubscriptions = true,
                importPlaylists = true,
                importPlaylistItems = true,
                importFavorites = true,
                importWatchLater = true,
                importHistory = true,
            ),
        )
        val status = requireNotNull(api.youtubeTakeoutStatus(created.jobId).body())
        val report = requireNotNull(api.youtubeTakeoutReport(created.jobId).body())

        val upload = server.takeRequest()
        assertEquals("POST", upload.method)
        assertEquals("/imports/youtube-takeout", upload.path)
        assertTrue(upload.body.readUtf8().contains("name=\"archive\"; filename=\"takeout.zip\""))
        assertEquals("/imports/youtube-takeout/job-1/preview", server.takeRequest().path)
        val commit = server.takeRequest()
        assertEquals("/imports/youtube-takeout/job-1/commit", commit.path)
        assertEquals(
            """{"importSubscriptions":true,"importPlaylists":true,"importPlaylistItems":true,"importFavorites":true,"importWatchLater":true,"importHistory":true}""",
            commit.body.readUtf8(),
        )
        assertEquals("/imports/youtube-takeout/job-1", server.takeRequest().path)
        assertEquals("/imports/youtube-takeout/job-1/report", server.takeRequest().path)
        assertEquals(6, preview.counts.history)
        assertEquals("completed", status.status)
        assertEquals(5, report.history.imported)
    }

    private fun jsonResponse(body: String, code: Int = 200) = MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "application/json")
        .setBody(body)

    private companion object {
        const val JOB_STATUS =
            """{"jobId":"job-1","status":"completed","phase":"preview_ready","progress":100,"createdAt":1,"updatedAt":2,"expiresAt":3}"""
        const val PREVIEW =
            """{"counts":{"subscriptions":1,"playlists":2,"playlistItems":3,"favorites":4,"watchLater":5,"history":6},"dedup":{"subscriptions":0,"playlists":0,"playlistItems":1},"samples":{},"warnings":[],"errors":[],"issues":[],"issueSummary":{"total":0,"warnings":0,"errors":0}}"""
        const val REPORT =
            """{"subscriptions":{"imported":1,"skipped":0,"failed":0},"playlists":{"imported":2,"skipped":0,"failed":0},"playlistItems":{"imported":3,"skipped":1,"failed":0},"favorites":{"imported":4,"skipped":0,"failed":0},"watchLater":{"imported":5,"skipped":0,"failed":0},"history":{"imported":5,"skipped":1,"failed":0},"warnings":[],"errors":[],"issues":[],"issueSummary":{"total":0,"warnings":0,"errors":0},"finishedAt":4}"""
    }
}
