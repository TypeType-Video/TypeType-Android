package dev.typetype.android.feature.player.error

import dev.typetype.android.data.network.ServerError
import dev.typetype.android.data.network.ServerResponseException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamErrorCodesTest {
    private fun server(code: String?, status: Int) = ServerResponseException(ServerError("failure", code, status, "req"))

    @Test
    fun `each restriction keeps its own meaning`() {
        assertEquals(StreamErrorKind.PrivateContent, classifyStreamError(server("private_content", 403)).kind)
        assertEquals(StreamErrorKind.AgeRestricted, classifyStreamError(server("age_restricted", 403)).kind)
        assertEquals(StreamErrorKind.ContentBlocked, classifyStreamError(server("content_blocked", 403)).kind)
        assertEquals(StreamErrorKind.MemberOnly, classifyStreamError(server("members_only", 403)).kind)
    }

    @Test
    fun `server side trouble is never shown as a missing video or a lost connection`() {
        assertEquals(StreamErrorKind.ProviderBlocked, classifyStreamError(server("provider_access_blocked", 503)).kind)
        assertEquals(StreamErrorKind.ServerSlow, classifyStreamError(server("upstream_timeout", 504)).kind)
        assertEquals(StreamErrorKind.ServerSlow, classifyStreamError(server(null, 504)).kind)
        assertEquals(StreamErrorKind.ServerUnavailable, classifyStreamError(server(null, 502)).kind)
        assertEquals(StreamErrorKind.RateLimited, classifyStreamError(server(null, 429)).kind)
    }

    @Test
    fun `transport failures depend on whether the phone is online`() {
        val refused = IllegalStateException("load", ConnectException("refused"))
        val dns = IllegalStateException("load", UnknownHostException("watch.example"))
        val slow = IllegalStateException("load", SocketTimeoutException("read timed out"))

        assertEquals(StreamErrorKind.NetworkUnavailable, classifyStreamError(refused, networkAvailable = false).kind)
        assertEquals(StreamErrorKind.InstanceUnreachable, classifyStreamError(refused, networkAvailable = true).kind)
        assertEquals(StreamErrorKind.InstanceUnreachable, classifyStreamError(dns, networkAvailable = true).kind)
        assertEquals(StreamErrorKind.ServerSlow, classifyStreamError(slow, networkAvailable = true).kind)
    }

    @Test
    fun `only passing failures are retried before showing an error`() {
        assertTrue(isRetryablePlaybackFailure(server("provider_access_blocked", 503), networkAvailable = true))
        assertTrue(isRetryablePlaybackFailure(SocketTimeoutException(), networkAvailable = true))
        assertFalse(isRetryablePlaybackFailure(SocketTimeoutException(), networkAvailable = false))
        assertFalse(isRetryablePlaybackFailure(server("private_content", 403), networkAvailable = true))
        assertFalse(isRetryablePlaybackFailure(server("content_unavailable", 404), networkAvailable = true))
    }
}
