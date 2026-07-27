package dev.typetype.android.data.stream

import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.data.network.RetrofitFactory
import dev.typetype.android.data.network.TypeTypeApi
import dev.typetype.android.data.network.sabrControlClient
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
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

class StreamEndpointLoaderTest {
    private lateinit var server: MockWebServer
    private lateinit var api: TypeTypeApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = RetrofitFactory(
            sessionClient = OkHttpClient().sabrControlClient(),
            json = Json { ignoreUnknownKeys = true },
        ).create(server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun youtubeUsesSabrContractWithoutLegacyProbe() = runBlocking {
        server.enqueue(jsonResponse(sabr = true))

        val response = api.loadStreamResponse(YOUTUBE_URL)

        assertTrue(response.isSuccessful)
        assertTrue(response.body()?.hasPlayableSabrContract() == true)
        assertEquals("sabr", response.body()?.videoOnlyStreams?.single()?.deliveryMethod)
        assertEquals("/streams/youtube/sabr?url=${encode(YOUTUBE_URL)}", server.takeRequest().path)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun youtubePlaybackUsesSharedSabrBootstrapDirectly() = runBlocking {
        server.enqueue(jsonResponse(sabr = true))

        val response = api.loadYouTubeSabrBootstrapResponse(YOUTUBE_URL)

        assertTrue(response.isSuccessful)
        assertTrue(response.body()?.hasPlayableSabrContract(server.url("/").toString()) == true)
        assertEquals("/streams/youtube/sabr/bootstrap?url=${encode(YOUTUBE_URL)}", server.takeRequest().path)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun youtubeHlsResponseIsNotAcceptedAsSabr() = runBlocking {
        server.enqueue(jsonResponse(sabr = false, hls = true))

        val response = api.loadStreamResponse(YOUTUBE_URL)

        assertTrue(response.isSuccessful)
        assertFalse(response.body()?.hasPlayableSabrContract() == true)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun youtubeSabrResponseWithoutValidItagsIsRejected() = runBlocking {
        server.enqueue(jsonResponse(sabr = true, videoItag = 0))

        val response = api.loadStreamResponse(YOUTUBE_URL)

        assertTrue(response.isSuccessful)
        assertFalse(response.body()?.hasPlayableSabrContract(server.url("/").toString()) == true)
    }

    @Test
    fun youtubeSabrResponseWithOffOriginManifestIsRejected() = runBlocking {
        server.enqueue(jsonResponse(sabr = true, manifestUrl = "https://media.example/manifest"))

        val response = api.loadStreamResponse(YOUTUBE_URL)

        assertTrue(response.isSuccessful)
        assertFalse(response.body()?.hasPlayableSabrContract(server.url("/").toString()) == true)
    }

    @Test
    fun youtubeSabrResponseWithOpusAudioIsRejected() = runBlocking {
        server.enqueue(jsonResponse(sabr = true, audioMimeType = "audio/webm", audioCodec = "opus"))

        val response = api.loadStreamResponse(YOUTUBE_URL)

        assertTrue(response.isSuccessful)
        assertFalse(response.body()?.hasPlayableSabrContract(server.url("/").toString()) == true)
    }

    @Test
    fun youtubeSabrResponseWithUnsupportedVideoCodecIsRejected() = runBlocking {
        server.enqueue(jsonResponse(sabr = true, videoCodec = "hvc1.1.6.L120"))

        val response = api.loadStreamResponse(YOUTUBE_URL)

        assertTrue(response.isSuccessful)
        assertFalse(response.body()?.hasPlayableSabrContract(server.url("/").toString()) == true)
    }

    @Test
    fun youtubeNeverFallsBackWhenSabrIsUnavailable() = runBlocking {
        server.enqueue(errorResponse(422))

        val response = api.loadStreamResponse(YOUTUBE_URL)

        assertEquals(422, response.code())
        assertEquals("/streams/youtube/sabr?url=${encode(YOUTUBE_URL)}", server.takeRequest().path)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun youtubeSabrNeverFollowsRedirects() = runBlocking {
        val external = MockWebServer()
        external.start()
        try {
            server.enqueue(
                MockResponse().setResponseCode(307).setHeader(
                    "Location",
                    external.url("/streams/youtube/sabr"),
                ),
            )
            external.enqueue(jsonResponse(sabr = true))

            val failure = runCatching { api.loadStreamResponse(YOUTUBE_URL) }.exceptionOrNull()

            assertEquals("youtube_sabr_contract_mismatch", (failure as CodedFailure).failureCode)
            assertEquals(1, server.requestCount)
            assertEquals(0, external.requestCount)
        } finally {
            external.shutdown()
        }
    }

    @Test
    fun olderServerFallsBackFromProviderRouteToGenericRoute() = runBlocking {
        server.enqueue(errorResponse(404))
        server.enqueue(jsonResponse(sabr = false))

        val response = api.loadStreamResponse(NICONICO_URL)

        assertTrue(response.isSuccessful)
        assertEquals("/streams/niconico?url=${encode(NICONICO_URL)}", server.takeRequest().path)
        assertEquals("/streams?url=${encode(NICONICO_URL)}", server.takeRequest().path)
    }

    private fun jsonResponse(
        sabr: Boolean,
        hls: Boolean = false,
        videoItag: Int = 137,
        manifestUrl: String = "/sabr/manifest/video",
        videoCodec: String = "avc1.640028",
        audioMimeType: String = "audio/mp4",
        audioCodec: String = "mp4a.40.2",
    ): MockResponse = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(streamJson(sabr, hls, videoItag, manifestUrl, videoCodec, audioMimeType, audioCodec))

    private fun errorResponse(code: Int): MockResponse = MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "application/json")
        .setBody("{\"error\":\"fixture unavailable\"}")

    private fun streamJson(
        sabr: Boolean,
        hls: Boolean,
        videoItag: Int,
        manifestUrl: String,
        videoCodec: String,
        audioMimeType: String,
        audioCodec: String,
    ): String {
        val delivery = if (sabr) "sabr" else "progressive"
        val manifest = if (sabr) "\"$manifestUrl\"" else "null"
        val mediaUrl = if (sabr) "" else "https://media.example/video.mp4"
        val hlsUrl = if (hls) "https://media.example/live.m3u8" else ""
        return """
            {
              "id":"video","title":"Video","uploaderName":"Channel","uploaderUrl":"/channel",
              "uploaderAvatarUrl":"","thumbnailUrl":"","description":"","duration":60,
              "viewCount":1,"likeCount":0,"dislikeCount":0,"uploadDate":"","uploaded":-1,
              "uploaderSubscriberCount":0,"uploaderVerified":false,"category":"","license":"",
              "visibility":"public","streamType":"VIDEO_STREAM","isShortFormContent":false,
              "requiresMembership":false,"startPosition":0,"hlsUrl":"$hlsUrl","dashMpdUrl":"",
              "videoStreams":[],
              "videoOnlyStreams":[{"url":"$mediaUrl","mimeType":"video/mp4","format":"MPEG_4",
                "resolution":"1080p","codec":"$videoCodec","isVideoOnly":true,"itag":$videoItag,
                "width":1920,"height":1080,"fps":30,"contentLength":1,
                "deliveryMethod":"$delivery","manifestUrl":$manifest}],
              "audioStreams":[{"url":"$mediaUrl","mimeType":"$audioMimeType","format":"MPEG_4",
                "codec":"$audioCodec","itag":140,"contentLength":1,"isOriginal":true,
                "deliveryMethod":"$delivery","manifestUrl":$manifest}],
              "subtitles":[],"relatedStreams":[]
            }
        """.trimIndent()
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")

    private companion object {
        const val YOUTUBE_URL = "https://www.youtube.com/watch?v=video"
        const val NICONICO_URL = "https://www.nicovideo.jp/watch/sm9"
    }
}
