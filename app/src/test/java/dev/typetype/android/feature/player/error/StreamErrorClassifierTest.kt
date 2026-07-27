package dev.typetype.android.feature.player.error

import dev.typetype.android.data.network.ServerError
import dev.typetype.android.data.network.ServerResponseException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class StreamErrorClassifierTest {
    @Test
    fun `classifies server YouTube verification message`() {
        val message = "Sign in is required to verify access to this video"

        val result = classifyStreamError(message)

        assertEquals(StreamErrorKind.YouTubeSessionRequired, result.kind)
        assertTrue(isYouTubeSessionRequiredMessage(message))
        assertFalse(isMemberOnlyMessage(message))
    }

    @Test
    fun `keeps members only content distinct`() {
        val result = classifyStreamError("This video is only available for members")

        assertEquals(StreamErrorKind.MemberOnly, result.kind)
    }

    @Test
    fun `does not expose an unknown server message as a known class`() {
        val result = classifyStreamError("Unexpected extractor detail")

        assertEquals(StreamErrorKind.Generic, result.kind)
    }

    @Test
    fun `uses stable server code before extractor wording`() {
        val failure = ServerResponseException(
            ServerError(
                message = "Wording can change",
                code = "youtube_session_needs_reconnect",
                statusCode = 400,
                requestId = "request-1",
            ),
        )

        val result = classifyStreamError(failure)

        assertEquals(StreamErrorKind.YouTubeSessionRequired, result.kind)
    }

    @Test
    fun `identifies a failed SABR contract from its stable code`() {
        val failure = ServerResponseException(
            ServerError(
                message = "No playable source",
                code = "no_playable_streams",
                statusCode = 422,
                requestId = "request-2",
            ),
        )

        val result = classifyStreamError(failure)

        assertEquals(StreamErrorKind.SabrUnavailable, result.kind)
    }

    @Test
    fun `classifies the server bootstrap failure fallback message`() {
        val failure = ServerResponseException(
            ServerError(
                message = "SABR bootstrap metadata unavailable",
                code = "error",
                statusCode = 422,
                requestId = "request-bootstrap",
            ),
        )

        val result = classifyStreamError(failure)

        assertEquals(StreamErrorKind.SabrUnavailable, result.kind)
        assertEquals("request-bootstrap", result.requestId)
    }

    @Test
    fun `keeps unsupported Android live playback distinct`() {
        val failure = ServerResponseException(
            ServerError(
                message = "Live playback is unsupported",
                code = "android_live_playback_unsupported",
                statusCode = 422,
                requestId = "request-3",
            ),
        )

        val result = classifyStreamError(failure)

        assertEquals(StreamErrorKind.LiveUnsupported, result.kind)
    }

    @Test
    fun `classifies stable availability codes like the web client`() {
        val scheduled = ServerResponseException(
            ServerError("Premieres in 2 days", "scheduled_premiere", 422, "request-4"),
        )
        val paid = ServerResponseException(
            ServerError("Purchase required", "paid_content", 400, "request-5"),
        )
        val members = ServerResponseException(
            ServerError("Join this channel", "members_only", 400, "request-6"),
        )

        assertEquals(StreamErrorKind.ScheduledPremiere, classifyStreamError(scheduled).kind)
        assertEquals(StreamErrorKind.PaidContent, classifyStreamError(paid).kind)
        assertEquals(StreamErrorKind.MemberOnly, classifyStreamError(members).kind)
    }

    @Test
    fun `finds network failures wrapped by another layer`() {
        val failure = IllegalStateException("Request failed", IOException("socket closed"))

        assertEquals(StreamErrorKind.NetworkUnavailable, classifyStreamError(failure).kind)
    }

    @Test
    fun `identifies an expired authentication session`() {
        val failure = ServerResponseException(
            ServerError("Unauthorized", null, 401, "request-auth"),
        )

        val result = classifyStreamError(failure)

        assertEquals(StreamErrorKind.AuthenticationExpired, result.kind)
        assertEquals("request-auth", result.requestId)
    }

    @Test
    fun `keeps a temporarily unavailable subtitle catalog actionable`() {
        val failure = ServerResponseException(
            ServerError(
                message = "Subtitle inventory unavailable",
                code = "android_subtitle_inventory_unavailable",
                statusCode = 503,
                requestId = "request-subtitles",
            ),
        )

        val result = classifyStreamError(failure)

        assertEquals(StreamErrorKind.SubtitleInventoryUnavailable, result.kind)
        assertEquals("request-subtitles", result.requestId)
    }

    @Test
    fun `keeps Android preparation terminal failures distinct`() {
        val timeout = serverFailure("android_playback_preparation_timeout", 503, "request-timeout")
        val failed = serverFailure("android_playback_preparation_failed", 503, "request-failed")
        val invalid = serverFailure("android_playback_invalid_index", 422, "request-index")

        assertEquals(StreamErrorKind.SabrPreparationTimedOut, classifyStreamError(timeout).kind)
        assertEquals(StreamErrorKind.SabrPreparationFailed, classifyStreamError(failed).kind)
        assertEquals(StreamErrorKind.SabrInvalidIndex, classifyStreamError(invalid).kind)
        assertEquals("request-failed", classifyStreamError(failed).requestId)
    }

    private fun serverFailure(code: String, status: Int, requestId: String) =
        ServerResponseException(
            ServerError(
                message = "Stable wording is not required",
                code = code,
                statusCode = status,
                requestId = requestId,
            ),
        )
}
