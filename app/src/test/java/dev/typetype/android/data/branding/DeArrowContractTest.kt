package dev.typetype.android.data.branding

import dev.typetype.android.data.network.RetrofitFactory
import dev.typetype.android.data.network.TypeTypeApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class DeArrowContractTest {
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
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `uses the server DeArrow contract and keeps candidate arrays`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(PAYLOAD),
        )

        val response = api.deArrow(VIDEO_ID)
        val request = server.takeRequest()
        val item = requireNotNull(response.body()).toDomain(server.url("/api/").toString(), 200)

        assertEquals("/api/dearrow?videoId=$VIDEO_ID", request.path)
        assertEquals("Community title", item.titles?.single()?.title)
        assertEquals(
            server.url("/api/dearrow/thumbnail?videoId=$VIDEO_ID&time=12.5").toString(),
            item.thumbnails?.single()?.thumbnailUrl,
        )
        assertEquals(
            server.url("/api/dearrow/thumbnail?videoId=$VIDEO_ID&time=50.0").toString(),
            item.neutralThumbnailUrl,
        )
        assertNull(item.legacyThumbnailUrl)
    }

    @Test
    fun `recognizes only supported YouTube sources`() {
        val sources = listOf(
            VIDEO_ID,
            "https://www.youtube.com/watch?v=$VIDEO_ID",
            "https://youtu.be/$VIDEO_ID",
            "https://m.youtube.com/shorts/$VIDEO_ID",
            "https://youtube.com/embed/$VIDEO_ID",
            "https://youtube.com/live/$VIDEO_ID",
        )

        sources.forEach { assertEquals(VIDEO_ID, youtubeVideoId(it)) }
        assertNull(youtubeVideoId("https://youtube.example/watch?v=$VIDEO_ID"))
        assertNull(youtubeVideoId("https://example.com/$VIDEO_ID"))
        assertNull(youtubeVideoId("invalid"))
    }

    @Test
    fun `rejects thumbnail URLs outside the selected instance`() {
        val dto = dev.typetype.android.data.network.dto.DeArrowDto(
            videoId = VIDEO_ID,
            thumbnailUrl = "https://images.example/legacy.jpg",
            thumbnails = listOf(
                dev.typetype.android.data.network.dto.DeArrowThumbnailCandidateDto(
                    thumbnailUrl = "https://images.example/community.jpg",
                    original = false,
                    votes = 1,
                    locked = false,
                ),
            ),
        )

        val item = dto.toDomain(server.url("/api/").toString(), 0)

        assertNull(item.legacyThumbnailUrl)
        assertNull(item.thumbnails?.single()?.thumbnailUrl)
    }

    private companion object {
        const val VIDEO_ID = "dQw4w9WgXcQ"
        val PAYLOAD = """
            {
              "videoId": "$VIDEO_ID",
              "title": "Community title",
              "thumbnailUrl": "https://images.example/legacy.jpg",
              "titles": [{
                "title": "Community title",
                "original": false,
                "votes": 2,
                "locked": false,
                "uuid": "title"
              }],
              "thumbnails": [{
                "thumbnailUrl": "/dearrow/thumbnail?videoId=$VIDEO_ID&time=12.5",
                "original": false,
                "votes": 1,
                "locked": false,
                "uuid": "thumbnail"
              }],
              "randomTime": 0.25,
              "videoDuration": 200,
              "attributionUrl": "https://dearrow.ajay.app"
            }
        """.trimIndent()
    }
}
