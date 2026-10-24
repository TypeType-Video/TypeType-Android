package dev.typetype.android.data.stream

import dev.typetype.android.domain.stream.SabrPlaybackBufferedRange
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SabrPlaybackSessionPreparerTest {
    private val script = SabrServerScript()
    private val server = script.server
    private val api = script.api
    private val baseUrl = script.baseUrl

    @After
    fun tearDown() = script.shutdown()

    @Test
    fun sharedSessionUsesPositionPrefetchAndSegmentsBeforeMedia3() = runBlocking {
        script.enqueueCreate(startTimeMs = 42_000)
        script.enqueuePosition(42_000)
        script.enqueuePrefetch(ready = false)
        script.enqueuePrefetch(ready = true)
        script.enqueueWindow(startTimeMs = 40_000, endOfStream = false)

        val session = preparer().prepare(api, baseUrl, script.target(), 42_000)

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
        script.enqueueCreate(startTimeMs = 91_000, ready = true)
        script.enqueuePosition(91_000)
        script.enqueuePrefetch(ready = true)
        script.enqueueWindow(startTimeMs = 90_000, endOfStream = true, segmentStartMs = 90_000)

        preparer().prepare(api, baseUrl, script.target(), 91_000)

        val body = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertEquals("91000", body.getValue("startTimeMs").jsonPrimitive.content)
        assertEquals("91000", body.getValue("playerTimeMs").jsonPrimitive.content)
        assertEquals("137", body.getValue("videoItag").jsonPrimitive.content)
        assertEquals("140", body.getValue("audioItag").jsonPrimitive.content)
    }

    @Test
    fun prewarmCreationMarksTheSessionForReuse() = runBlocking {
        script.enqueueCreate(startTimeMs = 0, ready = true)
        script.enqueuePosition(0)
        script.enqueuePrefetch(ready = true)
        script.enqueueWindow(startTimeMs = 0, endOfStream = false)

        preparer().prepare(api, baseUrl, script.target(), 0L, prewarm = true)

        assertEquals("/api/sabr/playback/video?prewarm=true", server.takeRequest().path)
    }

    @Test
    fun playbackCreationOmitsThePrewarmFlag() = runBlocking {
        script.enqueueCreate(startTimeMs = 0, ready = true)
        script.enqueuePosition(0)
        script.enqueuePrefetch(ready = true)
        script.enqueueWindow(startTimeMs = 0, endOfStream = false)

        preparer().prepare(api, baseUrl, script.target(), 0L)

        assertEquals("/api/sabr/playback/video", server.takeRequest().path)
    }

    @Test
    fun refreshReportsBufferedRangesForBothSelectedTracks() = runBlocking {
        script.enqueuePosition(55_000)
        script.enqueuePrefetch(ready = true)
        script.enqueueWindow(startTimeMs = 50_000, endOfStream = false, segmentStartMs = 50_000)

        val session = preparer().refresh(
            api = api,
            baseUrl = baseUrl,
            target = script.target(),
            binding = script.binding(),
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
        script.enqueuePosition(playerTimeMs)
        script.enqueuePrefetch(ready = false)
        script.enqueuePrefetch(ready = true)
        script.enqueueWindow(startTimeMs = 65_000, endOfStream = false, segmentStartMs = 65_000)
        val preparer = SabrPlaybackSessionPreparer(
            pause = {
                playerTimeMs = 66_000L
                bufferedEndMs = 69_000L
            },
            maxWindowPolls = 4,
        )

        preparer.refresh(api, baseUrl, script.target(), script.binding()) {
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
        script.enqueueCreate(startTimeMs = 60_000, ready = true, generation = 1)
        script.enqueuePosition(60_000, generation = 1)
        script.enqueuePrefetch(ready = true, generation = 1)
        script.enqueueWindow(
            startTimeMs = 60_000,
            endOfStream = false,
            segmentStartMs = 60_000,
            generation = 1,
        )

        val session = preparer().seek(api, baseUrl, script.target(), script.binding(), 60_000)

        assertEquals("session", session.sessionId)
        assertEquals(1, session.generation)
        assertEquals("/api/sabr/playback/session/seek", server.takeRequest().path)
        assertEquals("/api/sabr/playback/session/position", server.takeRequest().path)
    }

    @Test
    fun seekLeavesFreshSessionRecoveryToThePlaybackService() = runBlocking {
        script.enqueueCreate(startTimeMs = 60_000, ready = true, generation = 1)
        script.enqueuePosition(60_000, generation = 1)
        script.enqueue(
            """{"sessionId":"session","generation":1,"ready":false,"status":"failed","terminalError":"reload failed","recoveryAction":"retry_fresh_session"}""",
        )
        script.enqueueCreate(startTimeMs = 60_000, ready = true, sessionId = "fresh")
        script.enqueuePosition(60_000, sessionId = "fresh")
        script.enqueuePrefetch(ready = true, sessionId = "fresh")
        script.enqueueWindow(startTimeMs = 60_000, endOfStream = false, sessionId = "fresh")

        val failure = runCatching {
            preparer().seek(api, baseUrl, script.target(), script.binding(), 60_000)
        }.exceptionOrNull()

        assertTrue(failure is SabrPlaybackRecoveryException)
        assertEquals(3, server.requestCount)
    }

    @Test
    fun serverRequestedLowerItagMustBelongToTheAndroidRecoverySet() = runBlocking {
        script.enqueueCreate()
        script.enqueuePosition(0)
        script.enqueue(
            """{"sessionId":"session","generation":0,"ready":false,"status":"failed","terminalError":"retry lower","recoveryAction":"retry_fresh_session_lower_video_itag","retryVideoItags":[136]}""",
        )
        script.enqueueCreate(ready = true, sessionId = "fresh", videoItag = 136)
        script.enqueuePosition(0, sessionId = "fresh")
        script.enqueuePrefetch(ready = true, sessionId = "fresh")
        script.enqueueWindow(0, false, sessionId = "fresh", videoItag = 136)

        val session = preparer().prepare(
            api,
            baseUrl,
            script.target(recoveryVideoItags = setOf(136)),
        )

        assertEquals(136, session.videoItag)
        repeat(3) { server.takeRequest() }
        val retryBody = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
        assertEquals("136", retryBody.getValue("videoItag").jsonPrimitive.content)
    }

    @Test
    fun terminalWindowFailureStopsPolling() = runBlocking {
        script.enqueueCreate()
        script.enqueuePosition(0)
        script.enqueue(
            """{"sessionId":"session","generation":0,"ready":false,"retryAfterMs":250,"status":"failed","terminalError":"reload failed"}""",
        )

        val failure = runCatching { preparer().prepare(api, baseUrl, script.target()) }.exceptionOrNull()

        assertTrue(failure is SabrPlaybackRecoveryException)
        assertEquals(
            "youtube_sabr_window_failed",
            (failure as SabrPlaybackRecoveryException).failureCode,
        )
        assertEquals(3, server.requestCount)
    }

    @Test
    fun windowMediaMustStayOnTheTypeTypeOrigin() = runBlocking {
        script.enqueueCreate()
        script.enqueuePosition(0)
        script.enqueuePrefetch(ready = true)
        script.enqueueWindow(startTimeMs = 0, endOfStream = false, audioInitUrl = "https://media.example/init")

        val failure = runCatching { preparer().prepare(api, baseUrl, script.target()) }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertTrue(failure?.message.orEmpty().contains("origin"))
    }

    private fun preparer() = SabrPlaybackSessionPreparer(pause = {}, maxWindowPolls = 4)
}
