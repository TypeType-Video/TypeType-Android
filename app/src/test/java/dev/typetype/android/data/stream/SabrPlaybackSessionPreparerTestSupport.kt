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
import org.junit.After
import org.junit.Before

abstract class SabrPlaybackSessionPreparerTestSupport {
    protected lateinit var server: MockWebServer
    protected lateinit var api: TypeTypeApi
    protected lateinit var baseUrl: String

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

    internal fun preparer() = SabrPlaybackSessionPreparer(pause = {}, maxWindowPolls = 4)

    protected fun target(
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

    protected fun binding() = SabrPlaybackBinding("session", 0, 137, 140, "en.0")

    protected fun enqueueCreate(
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

    protected fun enqueuePosition(
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

    protected fun enqueuePrefetch(
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

    protected fun enqueueWindow(
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

    protected fun jsonResponse(body: String, code: Int = 200) = MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}
