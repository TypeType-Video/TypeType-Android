package dev.typetype.android.data.stream

import dev.typetype.android.data.network.RetrofitFactory
import dev.typetype.android.data.network.TypeTypeApi
import dev.typetype.android.data.network.sabrControlClient
import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import dev.typetype.android.domain.stream.StreamRequestScope
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

// Queues the control responses of a TypeType server for one SABR video, in the order the client asks for them.
internal class SabrServerScript {
    val server = MockWebServer().apply { start() }
    val baseUrl: String = server.url("/api/").toString()
    val api: TypeTypeApi = RetrofitFactory(
        sessionClient = OkHttpClient().sabrControlClient(),
        json = Json { ignoreUnknownKeys = true },
    ).create(baseUrl)

    fun shutdown() = server.shutdown()

    fun target(
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

    fun binding(sessionId: String = "session") = SabrPlaybackBinding(sessionId, 0, 137, 140, "en.0")

    fun enqueueCreate(
        startTimeMs: Long = 0,
        ready: Boolean = false,
        sessionId: String = "session",
        generation: Long = 0,
        videoItag: Int = 137,
    ) {
        val manifest = if (ready) "\"/sabr/playback/$sessionId/manifest\"" else "null"
        val status = if (ready) "ready" else "preparing"
        val retry = if (ready) "null" else "250"
        enqueue(
            """{"sessionId":"$sessionId","videoId":"video","manifestUrl":$manifest,"videoItag":$videoItag,""" +
                """"audioItag":140,"audioTrackId":"en.0","startTimeMs":$startTimeMs,"generation":$generation,""" +
                """"ready":$ready,"status":"$status","retryAfterMs":$retry}""",
            if (ready) 200 else 202,
        )
    }

    fun enqueuePosition(playerTimeMs: Long, sessionId: String = "session", generation: Long = 0) {
        enqueue(
            """{"sessionId":"$sessionId","generation":$generation,"playerTimeMs":$playerTimeMs,""" +
                """"readerHeadMs":0,"readerTailMs":0,"bufferedEdgeMs":70000}""",
        )
    }

    // A pending prefetch keeps the same buffered edge, so repeating it is what the client reads as a stuck session.
    fun enqueuePrefetch(ready: Boolean, sessionId: String = "session", generation: Long = 0) {
        val retry = if (ready) "null" else "250"
        val status = if (ready) "ready" else "preparing"
        enqueue(
            """{"sessionId":"$sessionId","generation":$generation,"ready":$ready,"retryAfterMs":$retry,""" +
                """"status":"$status","bufferedEdgeMs":70000}""",
            if (ready) 200 else 202,
        )
    }

    fun enqueueWindow(
        startTimeMs: Long,
        endOfStream: Boolean,
        segmentStartMs: Long = 40_000,
        audioInitUrl: String = "/api/sabr/playback/session/140/init?generation=0",
        sessionId: String = "session",
        generation: Long = 0,
        videoItag: Int = 137,
    ) {
        val end = segmentStartMs + 30_000
        val audioInit = if (audioInitUrl.contains("media.example")) {
            audioInitUrl
        } else {
            "/api/sabr/playback/$sessionId/140/init?generation=$generation"
        }
        val path = "/api/sabr/playback/$sessionId"
        enqueue(
            """{"sessionId":"$sessionId","generation":$generation,"ready":true,"retryAfterMs":null,""" +
                """"durationMs":120000,"endOfStream":$endOfStream,"startTimeMs":$startTimeMs,""" +
                """"audio":{"mime":"audio/mp4","initUrl":"$audioInit","segments":[{"url":""" +
                """"$path/140/segment/5?generation=$generation","startMs":$segmentStartMs,""" +
                """"durationMs":${end - segmentStartMs}}]},"video":{"mime":"video/mp4","initUrl":""" +
                """"$path/$videoItag/init?generation=$generation","segments":[{"url":""" +
                """"$path/$videoItag/segment/5?generation=$generation","startMs":$segmentStartMs,""" +
                """"durationMs":${end - segmentStartMs}}]}}""",
        )
    }

    fun enqueue(body: String, code: Int = 200) {
        server.enqueue(
            MockResponse()
                .setResponseCode(code)
                .setHeader("Content-Type", "application/json")
                .setBody(body),
        )
    }

    fun requestPaths(count: Int): List<String> = List(count) { server.takeRequest().path.orEmpty() }
}
