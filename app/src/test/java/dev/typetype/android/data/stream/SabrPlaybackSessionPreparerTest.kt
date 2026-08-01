package dev.typetype.android.data.stream

import dev.typetype.android.data.network.RetrofitFactory
import dev.typetype.android.data.network.TypeTypeApi
import dev.typetype.android.data.network.sabrControlClient
import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackBufferedRange
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import dev.typetype.android.domain.stream.StreamRequestScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SabrPlaybackSessionPreparerTest {
    private lateinit var server: MockWebServer
    private lateinit var api: TypeTypeApi
    private lateinit var baseUrl: String

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        baseUrl = server.url("/api/").toString()
        api = RetrofitFactory(
            sessionClient = OkHttpClient().sabrControlClient(),
            json = Json { ignoreUnknownKeys = true },
        ).create(baseUrl)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun sharedSessionUsesPositionPrefetchAndSegmentsBeforeMedia3() = runBlocking {
        enqueueCreate(startTimeMs = 42_000)
        enqueuePosition(42_000)
        enqueuePrefetch(ready = false)
        enqueuePrefetch(ready = true)
        enqueueWindow(startTimeMs = 40_000, endOfStream = false)

        val session = preparer().prepare(api, baseUrl, target(), 42_000)

        assertEquals(server.url("/api/sabr/playback/session/manifest").toString(), session.manifestUrl)
        assertEquals(40_000, session.startTimeMs)
        assertEquals(70_000, session.windowEndMs)
        assertEquals(120_000, session.durationMs)
        assertFalse(session.endOfStream)
        assertEquals("/api/sabr/playback/video", server.takeRequest().path)
        assertEquals("/api/sabr/playback/session/position", server.takeRequest().path)
        assertEquals("/api/sabr/playback/session/prefetch", server.takeRequest().path)
        assertEquals("/api/sabr/playback/session/prefetch", server.takeRequest().path)
        assertEquals("/api/sabr/playback/session/segments", server.takeRequest().path)
    }

    @Test
    fun initialPositionIsSentInTheCreationBody() = runBlocking {
        enqueueCreate(startTimeMs = 91_000, ready = true)
        enqueuePosition(91_000)
        enqueuePrefetch(ready = true)
        enqueueWindow(startTimeMs = 90_000, endOfStream = true, segmentStartMs = 90_000)

        preparer().prepare(api, baseUrl, target(), 91_000)

        val body = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertEquals("91000", body.getValue("startTimeMs").jsonPrimitive.content)
        assertEquals("91000", body.getValue("playerTimeMs").jsonPrimitive.content)
        assertEquals("137", body.getValue("videoItag").jsonPrimitive.content)
        assertEquals("140", body.getValue("audioItag").jsonPrimitive.content)
    }

    @Test
    fun refreshReportsBufferedRangesForBothSelectedTracks() = runBlocking {
        enqueuePosition(55_000)
        enqueuePrefetch(ready = true)
        enqueueWindow(startTimeMs = 50_000, endOfStream = false, segmentStartMs = 50_000)

        val session = preparer().refresh(
            api = api,
            baseUrl = baseUrl,
            target = target(),
            binding = binding(),
            playerTimeMs = 55_000,
            bufferedRanges = listOf(
                SabrPlaybackBufferedRange(137, 45_000, 70_000),
                SabrPlaybackBufferedRange(140, 45_000, 70_000),
            ),
        )

        assertEquals(80_000, session.windowEndMs)
        server.takeRequest()
        val prefetch = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertEquals(2, prefetch.getValue("bufferedRanges").toString().count { it == '{' })
    }

    @Test
    fun refreshResamplesPlaybackStateWhileTheWindowIsPending() = runBlocking {
        var playerTimeMs = 37_000L
        var bufferedEndMs = 52_000L
        enqueuePosition(playerTimeMs)
        enqueuePrefetch(ready = false)
        enqueuePrefetch(ready = true)
        enqueueWindow(startTimeMs = 65_000, endOfStream = false, segmentStartMs = 65_000)
        val preparer = SabrPlaybackSessionPreparer(
            pause = {
                playerTimeMs = 66_000L
                bufferedEndMs = 69_000L
            },
            maxWindowPolls = 4,
        )

        preparer.refresh(api, baseUrl, target(), binding()) {
            dev.typetype.android.domain.stream.SabrPlaybackSnapshot(
                playerTimeMs = playerTimeMs,
                bufferedRanges = listOf(
                    SabrPlaybackBufferedRange(137, 14_000, bufferedEndMs),
                    SabrPlaybackBufferedRange(140, 13_500, bufferedEndMs),
                ),
            )
        }

        val requests = List(4) {
            Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        }
        assertEquals(
            listOf("37000", "37000", "66000", "66000"),
            requests.map { it.getValue("playerTimeMs").jsonPrimitive.content },
        )
        assertEquals(
            "69000",
            requests.last()
                .getValue("bufferedRanges")
                .jsonArray
                .first()
                .jsonObject
                .getValue("endMs")
                .jsonPrimitive
                .content,
        )
    }

    @Test
    fun seekAdvancesTheGenerationWithoutChangingTheSession() = runBlocking {
        enqueueCreate(startTimeMs = 60_000, ready = true, generation = 1)
        enqueuePosition(60_000, generation = 1)
        enqueuePrefetch(ready = true, generation = 1)
        enqueueWindow(
            startTimeMs = 60_000,
            endOfStream = false,
            segmentStartMs = 60_000,
            generation = 1,
        )

        val session = preparer().seek(api, baseUrl, target(), binding(), 60_000)

        assertEquals("session", session.sessionId)
        assertEquals(1, session.generation)
        assertEquals("/api/sabr/playback/session/seek", server.takeRequest().path)
        assertEquals("/api/sabr/playback/session/position", server.takeRequest().path)
    }

    @Test
    fun seekLeavesFreshSessionRecoveryToThePlaybackService() = runBlocking {
        enqueueCreate(startTimeMs = 60_000, ready = true, generation = 1)
        enqueuePosition(60_000, generation = 1)
        server.enqueue(
            jsonResponse(
                """{"sessionId":"session","generation":1,"ready":false,"status":"failed","terminalError":"reload failed","recoveryAction":"retry_fresh_session"}""",
            ),
        )
        enqueueCreate(startTimeMs = 60_000, ready = true, sessionId = "fresh")
        enqueuePosition(60_000, sessionId = "fresh")
        enqueuePrefetch(ready = true, sessionId = "fresh")
        enqueueWindow(startTimeMs = 60_000, endOfStream = false, sessionId = "fresh")

        val failure = runCatching {
            preparer().seek(api, baseUrl, target(), binding(), 60_000)
        }.exceptionOrNull()

        assertTrue(failure is SabrPlaybackRecoveryException)
        assertEquals(3, server.requestCount)
    }

    @Test
    fun serverRequestedLowerItagMustBelongToTheAndroidRecoverySet() = runBlocking {
        enqueueCreate()
        enqueuePosition(0)
        server.enqueue(
            jsonResponse(
                """{"sessionId":"session","generation":0,"ready":false,"status":"failed","terminalError":"retry lower","recoveryAction":"retry_fresh_session_lower_video_itag","retryVideoItags":[136]}""",
            ),
        )
        enqueueCreate(ready = true, sessionId = "fresh", videoItag = 136)
        enqueuePosition(0, sessionId = "fresh")
        enqueuePrefetch(ready = true, sessionId = "fresh")
        enqueueWindow(0, false, sessionId = "fresh", videoItag = 136)

        val session = preparer().prepare(
            api,
            baseUrl,
            target(recoveryVideoItags = setOf(136)),
        )

        assertEquals(136, session.videoItag)
        repeat(3) { server.takeRequest() }
        val retryBody = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertEquals("136", retryBody.getValue("videoItag").jsonPrimitive.content)
    }

    @Test
    fun terminalWindowFailureStopsPolling() = runBlocking {
        enqueueCreate()
        enqueuePosition(0)
        server.enqueue(
            jsonResponse(
                """{"sessionId":"session","generation":0,"ready":false,"retryAfterMs":250,"status":"failed","terminalError":"reload failed"}""",
            ),
        )

        val failure = runCatching { preparer().prepare(api, baseUrl, target()) }.exceptionOrNull()

        assertTrue(failure is SabrPlaybackRecoveryException)
        assertEquals(
            "youtube_sabr_window_failed",
            (failure as SabrPlaybackRecoveryException).failureCode,
        )
        assertEquals(3, server.requestCount)
    }

    @Test
    fun windowMediaMustStayOnTheTypeTypeOrigin() = runBlocking {
        enqueueCreate()
        enqueuePosition(0)
        enqueuePrefetch(ready = true)
        enqueueWindow(startTimeMs = 0, endOfStream = false, audioInitUrl = "https://media.example/init")

        val failure = runCatching { preparer().prepare(api, baseUrl, target()) }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertTrue(failure?.message.orEmpty().contains("origin"))
    }

    private fun preparer() = SabrPlaybackSessionPreparer(pause = {}, maxWindowPolls = 4)

    private fun target(
        videoItag: Int = 137,
        recoveryVideoItags: Set<Int> = emptySet(),
    ) = SabrPlaybackTarget(
        videoId = "video",
        requestScope = StreamRequestScope("server", "account", baseUrl),
        videoItag = videoItag,
        audioItag = 140,
        audioTrackId = "en.0",
        recoveryVideoItags = recoveryVideoItags,
    )

    private fun binding() = SabrPlaybackBinding("session", 0, 137, 140, "en.0")

    private fun enqueueCreate(
        startTimeMs: Long = 0,
        ready: Boolean = false,
        sessionId: String = "session",
        generation: Long = 0,
        videoItag: Int = 137,
    ) {
        server.enqueue(
            jsonResponse(
                """{"sessionId":"$sessionId","videoId":"video","manifestUrl":${if (ready) "\"/sabr/playback/$sessionId/manifest\"" else "null"},"videoItag":$videoItag,"audioItag":140,"audioTrackId":"en.0","startTimeMs":$startTimeMs,"generation":$generation,"ready":$ready,"status":"${if (ready) "ready" else "preparing"}","retryAfterMs":${if (ready) "null" else "250"}}""",
                if (ready) 200 else 202,
            ),
        )
    }

    private fun enqueuePosition(
        playerTimeMs: Long,
        sessionId: String = "session",
        generation: Long = 0,
    ) {
        server.enqueue(
            jsonResponse(
                """{"sessionId":"$sessionId","generation":$generation,"playerTimeMs":$playerTimeMs,"readerHeadMs":0,"readerTailMs":0,"bufferedEdgeMs":70000}""",
            ),
        )
    }

    private fun enqueuePrefetch(
        ready: Boolean,
        sessionId: String = "session",
        generation: Long = 0,
    ) {
        server.enqueue(
            jsonResponse(
                """{"sessionId":"$sessionId","generation":$generation,"ready":$ready,"retryAfterMs":${if (ready) "null" else "250"},"status":"${if (ready) "ready" else "preparing"}","bufferedEdgeMs":70000}""",
                if (ready) 200 else 202,
            ),
        )
    }

    private fun enqueueWindow(
        startTimeMs: Long,
        endOfStream: Boolean,
        segmentStartMs: Long = 40_000,
        audioInitUrl: String = "/api/sabr/playback/session/140/init?generation=0",
        sessionId: String = "session",
        generation: Long = 0,
        videoItag: Int = 137,
    ) {
        val audioEnd = segmentStartMs + 30_000
        val videoEnd = segmentStartMs + 30_000
        val resolvedAudioInitUrl = if (audioInitUrl.contains("media.example")) {
            audioInitUrl
        } else {
            "/api/sabr/playback/$sessionId/140/init?generation=$generation"
        }
        server.enqueue(
            jsonResponse(
                """{"sessionId":"$sessionId","generation":$generation,"ready":true,"retryAfterMs":null,"durationMs":120000,"endOfStream":$endOfStream,"startTimeMs":$startTimeMs,"audio":{"mime":"audio/mp4","initUrl":"$resolvedAudioInitUrl","segments":[{"url":"/api/sabr/playback/$sessionId/140/segment/5?generation=$generation","startMs":$segmentStartMs,"durationMs":${audioEnd - segmentStartMs}}]},"video":{"mime":"video/mp4","initUrl":"/api/sabr/playback/$sessionId/$videoItag/init?generation=$generation","segments":[{"url":"/api/sabr/playback/$sessionId/$videoItag/segment/5?generation=$generation","startMs":$segmentStartMs,"durationMs":${videoEnd - segmentStartMs}}]}}""",
            ),
        )
    }

    private fun jsonResponse(body: String, code: Int = 200) = MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
