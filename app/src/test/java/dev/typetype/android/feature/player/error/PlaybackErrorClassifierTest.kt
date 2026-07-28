package dev.typetype.android.feature.player.error

import androidx.media3.common.PlaybackException
import dev.typetype.android.core.error.CodedFailure
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackErrorClassifierTest {
    @Test
    fun `recognizes verification message inside server error JSON`() {
        val result = classifyHttpPlaybackFailure(
            """{"error":"Sign in is required to verify access to this video"}""",
        )

        assertEquals(PlaybackFailureKind.YouTubeSessionRequired, result)
    }

    @Test
    fun `hides unknown proxy response behind media delivery family`() {
        val result = classifyHttpPlaybackFailure("""{"error":"Upstream returned 403"}""")

        assertEquals(PlaybackFailureKind.MediaDelivery, result)
    }

    @Test
    fun `classifies expired authentication from media response`() {
        val result = classifyHttpPlaybackFailure(responseBody = null, statusCode = 401)

        assertEquals(PlaybackFailureKind.AuthenticationExpired, result)
    }

    @Test
    fun `classifies missing or expired playback session`() {
        assertEquals(
            PlaybackFailureKind.PlaybackSessionExpired,
            classifyHttpPlaybackFailure(responseBody = null, statusCode = 404),
        )
        assertEquals(
            PlaybackFailureKind.PlaybackSessionExpired,
            classifyHttpPlaybackFailure(responseBody = null, statusCode = 410),
        )
    }

    @Test
    fun `classifies stale playback generation`() {
        val result = classifyHttpPlaybackFailure(responseBody = null, statusCode = 409)

        assertEquals(PlaybackFailureKind.PlaybackGenerationChanged, result)
    }

    @Test
    fun `reads a safe request id from response headers`() {
        val result = requestIdFromHeaders(mapOf("x-request-id" to listOf("req-123:retry")))

        assertEquals("req-123:retry", result)
    }

    @Test
    fun `rejects unsafe request id from response headers`() {
        val result = requestIdFromHeaders(mapOf("X-Request-ID" to listOf("bad request id")))

        assertEquals(null, result)
    }

    @Test
    fun `classifies network interruption`() {
        val result = classifyPlaybackErrorCode(
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        )

        assertEquals(PlaybackFailureKind.Network, result)
    }

    @Test
    fun `classifies unsupported decoder`() {
        val result = classifyPlaybackErrorCode(
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        )

        assertEquals(PlaybackFailureKind.UnsupportedFormat, result)
    }

    @Test
    fun `classifies stale live window separately`() {
        val result = classifyPlaybackErrorCode(PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW)

        assertEquals(PlaybackFailureKind.BehindLiveWindow, result)
    }

    @Test
    fun `classifies a rejected SABR manifest as a server contract failure`() {
        val failure = object : IOException(), CodedFailure {
            override val failureCode: String = "youtube_sabr_contract_mismatch"
            override val requestId: String? = null
            override val statusCode: Int? = null
        }

        val result = classifyPlaybackCause(
            IllegalStateException(failure),
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
        )

        assertEquals(PlaybackFailureKind.SabrServerContract, result)
    }

    @Test
    fun `classifies exhausted SABR recovery before generic network failures`() {
        val failure = object : IOException(), CodedFailure {
            override val failureCode: String = "youtube_sabr_recovery_exhausted"
            override val requestId: String? = null
            override val statusCode: Int? = null
        }

        val result = classifyPlaybackCause(
            IllegalStateException(failure),
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
        )

        assertEquals(PlaybackFailureKind.SabrRecoveryExhausted, result)
    }

    @Test
    fun `classifies coded server rejection as media delivery`() {
        val failure = object : IOException(), CodedFailure {
            override val failureCode: String = "error"
            override val requestId: String? = "request-422"
            override val statusCode: Int = 422
        }

        val result = classifyPlaybackCause(
            IllegalStateException(failure),
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
        )

        assertEquals(PlaybackFailureKind.MediaDelivery, result)
    }
}
