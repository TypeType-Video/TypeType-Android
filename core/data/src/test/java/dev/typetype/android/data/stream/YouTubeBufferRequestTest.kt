package dev.typetype.android.data.stream

import dev.typetype.android.domain.preferences.YOUTUBE_BUFFER_SECONDS
import dev.typetype.android.domain.stream.SabrPlaybackBufferedRange
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeBufferRequestTest {
    @Test
    fun selectedTargetReachesBothPrefetchAndSegmentsAndIsRetainedInTheSession() = runBlocking {
        for (seconds in YOUTUBE_BUFFER_SECONDS) {
            val script = SabrServerScript()
            try {
                script.enqueuePosition(55_000)
                script.enqueuePrefetch(true)
                script.enqueueWindow(50_000, false, segmentStartMs = 50_000)
                val session = SabrPlaybackSessionPreparer().refresh(
                    script.api, script.baseUrl, script.target().copy(youTubeBufferSeconds = seconds),
                    script.binding(), 55_000,
                    listOf(
                        SabrPlaybackBufferedRange(137, 45_000, 70_000),
                        SabrPlaybackBufferedRange(140, 45_000, 70_000),
                    ),
                )
                script.server.takeRequest()
                repeat(2) {
                    val request = Json.parseToJsonElement(script.server.takeRequest().body.readUtf8()).jsonObject
                    val wireGoal = request["bufferGoalMs"]?.jsonPrimitive?.content ?: "30000"
                    assertEquals((seconds * 1_000).toString(), wireGoal)
                }
                assertEquals(seconds, session.youTubeBufferSeconds)
            } finally {
                script.shutdown()
            }
        }
    }

    @Test
    fun startupAndLiveKeepTheirExistingShortWindowsEvenWithTheLargestChoice() {
        val response = dev.typetype.android.data.network.dto.SabrPlaybackResponse(
            sessionId = "session", videoId = "video", videoItag = 137, audioItag = 140,
            generation = 0, ready = true, status = "ready",
        )
        assertEquals(2_500L, response.windowRequest(emptyList(), youTubeBufferSeconds = 120).bufferGoalMs)
        assertEquals(8_000L, response.windowRequest(emptyList(), isLive = true, youTubeBufferSeconds = 120).bufferGoalMs)
    }
}
