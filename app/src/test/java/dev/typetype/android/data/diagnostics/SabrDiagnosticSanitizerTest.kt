package dev.typetype.android.data.diagnostics

import dev.typetype.android.domain.diagnostics.SabrDiagnosticDetail
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SabrDiagnosticSanitizerTest {
    private val sanitizer = SabrDiagnosticSanitizer(Json { ignoreUnknownKeys = true })

    @Test
    fun pendingSegmentIsReducedToSafeCategoriesAndProgress() {
        val token = "private-session-token"
        val request = request("https://example.test/api/sabr/playback/video-id")
        val initial = sanitizer.sanitize(
            route = "/sabr/playback/create",
            request = request,
            response = response(
                request,
                202,
                """
                {
                  "sessionId": "$token",
                  "generation": 4,
                  "ready": false,
                  "status": "PREPARING",
                  "blockedBy": "video:137:123 pending",
                  "bufferedEdgeMs": 1200
                }
                """.trimIndent(),
            ),
        )

        assertTrue(initial?.sessionFingerprint?.matches(Regex("s-[0-9a-f]{12}")) == true)
        assertEquals(4L, initial?.generation)
        assertEquals(SabrDiagnosticDetail.State.Preparing, initial?.state)
        assertEquals(SabrDiagnosticDetail.Track.Video, initial?.track)
        assertEquals(SabrDiagnosticDetail.Blocker.SegmentPending, initial?.blocker)
        assertEquals(SabrDiagnosticDetail.BufferProgress.Initial, initial?.bufferProgress)
        assertRedacted(initial, token, "137", "123")

        val stalled = sanitizer.sanitize(
            "/sabr/playback/create",
            request,
            response(
                request,
                202,
                """{"sessionId":"$token","bufferedEdgeMs":1200,"blockedBy":"video:137:123 pending"}""",
            ),
        )
        assertEquals(SabrDiagnosticDetail.BufferProgress.Stalled, stalled?.bufferProgress)

        val advanced = sanitizer.sanitize(
            "/sabr/playback/create",
            request,
            response(request, 202, """{"sessionId":"$token","bufferedEdgeMs":2400}"""),
        )
        assertEquals(SabrDiagnosticDetail.BufferProgress.Advanced, advanced?.bufferProgress)
    }

    @Test
    fun serverFailuresAreReducedToDistinctTerminalFamilies() {
        assertTerminal("protected no-media response", SabrDiagnosticDetail.Terminal.ProtectedNoMedia)
        assertTerminal("token binding rejected with 403", SabrDiagnosticDetail.Terminal.Token)
        assertTerminal("SABR demand stalled", SabrDiagnosticDetail.Terminal.SegmentStalled)
        assertTerminal("Expected UMP response", SabrDiagnosticDetail.Terminal.UmpResponse)
        assertTerminal("upstream network timeout", SabrDiagnosticDetail.Terminal.Upstream)
        assertTerminal("unrecognized private detail", SabrDiagnosticDetail.Terminal.Other)
    }

    @Test
    fun preparationBlockersAreReducedToDistinctFamilies() {
        assertBlocker("audio:140:5 discontinuity", SabrDiagnosticDetail.Blocker.Discontinuity)
        assertBlocker("video:137:12 window capped", SabrDiagnosticDetail.Blocker.WindowCapped)
        assertBlocker("protected no-media", SabrDiagnosticDetail.Blocker.ProtectedNoMedia)
        assertBlocker("token binding rejected", SabrDiagnosticDetail.Blocker.Token)
        assertBlocker("reload requested", SabrDiagnosticDetail.Blocker.Reload)
    }

    @Test
    fun sessionStatusUsesOnlyPathFingerprintAndClosedCategory() {
        val token = "path-session-secret"
        val request = request("https://example.test/api/sabr/playback/$token/prefetch")

        val missing = sanitizer.sanitize(
            "/sabr/playback/prefetch",
            request,
            response(request, 404, """{"message":"raw private server message"}"""),
        )
        assertEquals(SabrDiagnosticDetail.Terminal.MissingSession, missing?.terminal)
        assertRedacted(missing, token, "raw", "private")

        val stale = sanitizer.sanitize(
            "/sabr/playback/prefetch",
            request,
            response(request, 409, ""),
        )
        assertEquals(SabrDiagnosticDetail.Terminal.StaleGeneration, stale?.terminal)

        val expired = sanitizer.sanitize(
            "/sabr/playback/prefetch",
            request,
            response(request, 410, ""),
        )
        assertEquals(SabrDiagnosticDetail.Terminal.ExpiredSession, expired?.terminal)
    }

    @Test
    fun unrelatedAndMalformedCreateResponsesAreIgnored() {
        val request = request("https://example.test/api/settings")
        assertNull(
            sanitizer.sanitize(
                "/settings",
                request,
                response(request, 200, """{"sessionId":"secret"}"""),
            ),
        )

        val create = request("https://example.test/api/sabr/playback/video-id")
        assertNull(
            sanitizer.sanitize(
                "/sabr/playback/create",
                create,
                response(create, 202, "not-json"),
            ),
        )
        assertNull(
            sanitizer.sanitize(
                "/sabr/playback/create",
                create,
                response(create, 202, """{"sessionId":{"raw":"secret"}}"""),
            ),
        )
    }

    @Test
    fun transportFailureKeepsOnlyAPathSessionFingerprint() {
        val token = "transport-session-secret"
        val request = request("https://example.test/api/sabr/playback/$token/segments")

        val detail = sanitizer.sanitizeFailure("/sabr/playback/segments", request)

        assertRedacted(detail, token)
        assertNull(detail?.generation)
        assertNull(detail?.state)
        assertNull(sanitizer.sanitizeFailure("/settings", request))
    }

    private fun assertTerminal(
        message: String,
        expected: SabrDiagnosticDetail.Terminal,
    ) {
        val token = "terminal-session-secret"
        val request = request("https://example.test/api/sabr/playback/$token/prefetch")
        val detail = sanitizer.sanitize(
            "/sabr/playback/prefetch",
            request,
            response(
                request,
                503,
                """{"sessionId":"$token","terminalError":"$message","recoveryAction":"retry_fresh_session"}""",
            ),
        )

        assertEquals(expected, detail?.terminal)
        assertEquals(SabrDiagnosticDetail.Recovery.FreshSession, detail?.recovery)
        assertRedacted(detail, token, message)
    }

    private fun assertBlocker(message: String, expected: SabrDiagnosticDetail.Blocker) {
        val token = "blocker-session-secret"
        val request = request("https://example.test/api/sabr/playback/$token/prefetch")
        val detail = sanitizer.sanitize(
            "/sabr/playback/prefetch",
            request,
            response(
                request,
                202,
                """{"sessionId":"$token","status":"preparing","blockedBy":"$message"}""",
            ),
        )

        assertEquals(expected, detail?.blocker)
        assertRedacted(detail, token, message)
    }

    private fun assertRedacted(detail: SabrDiagnosticDetail?, vararg forbidden: String) {
        val summary = requireNotNull(detail).redactedSummary()
        forbidden.forEach { assertFalse(summary.contains(it, ignoreCase = true)) }
    }

    private fun request(url: String): Request = Request.Builder().url(url).build()

    private fun response(request: Request, code: Int, body: String): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("test")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
}
