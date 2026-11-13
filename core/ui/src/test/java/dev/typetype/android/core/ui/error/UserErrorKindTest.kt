package dev.typetype.android.core.ui.error

import dev.typetype.android.core.error.CodedFailure
import java.io.IOException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException
import org.junit.Assert.assertEquals
import org.junit.Test

class UserErrorKindTest {
    @Test
    fun `classifies transport authentication and permission failures`() {
        assertEquals(UserErrorKind.NetworkUnavailable, classifyUserError(IOException("offline"), online = false))
        assertEquals(UserErrorKind.SignInAgain, classifyUserError(serverFailure(401)))
        assertEquals(UserErrorKind.PermissionDenied, classifyUserError(serverFailure(403)))
    }

    @Test
    fun `an online transport failure is the instance, not the connection`() {
        assertEquals(UserErrorKind.InstanceUnreachable, classifyUserError(java.net.ConnectException("refused")))
        assertEquals(UserErrorKind.ServerSlow, classifyUserError(java.net.SocketTimeoutException("read")))
        assertEquals(UserErrorKind.ServerSlow, classifyUserError(null as String?, 504))
    }

    @Test
    fun `server codes keep their meaning`() {
        assertEquals(UserErrorKind.ProviderBlocked, classifyUserError("provider_access_blocked", 503))
        assertEquals(UserErrorKind.ContentBlocked, classifyUserError("content_blocked", 403))
        assertEquals(UserErrorKind.ContentUnavailable, classifyUserError("private_content", 403))
    }

    @Test
    fun `keeps TLS failures distinct from other transport failures`() {
        assertEquals(
            UserErrorKind.SecureConnectionFailed,
            classifyUserError(SSLHandshakeException("certificate rejected")),
        )
        assertEquals(
            UserErrorKind.SecureConnectionFailed,
            classifyUserError(IOException("probe failed", SSLPeerUnverifiedException("hostname"))),
        )
    }

    @Test
    fun `classifies conflict throttling and temporary server failures`() {
        assertEquals(UserErrorKind.Conflict, classifyUserError(serverFailure(409)))
        assertEquals(UserErrorKind.RateLimited, classifyUserError(serverFailure(429)))
        assertEquals(UserErrorKind.ServerUnavailable, classifyUserError(serverFailure(503)))
    }

    @Test
    fun `stable server code wins over an unusual status`() {
        val failure = ServerFailure("rate_limited", 418)

        assertEquals(UserErrorKind.RateLimited, classifyUserError(failure))
        assertEquals(
            UserErrorKind.NetworkUnavailable,
            classifyUserError("client_network_unavailable", null),
        )
    }

    @Test
    fun `keeps local instance errors distinct from missing content`() {
        assertEquals(
            UserErrorKind.InstanceUnavailable,
            classifyUserError(IllegalStateException("Instance not found")),
        )
        assertEquals(UserErrorKind.ContentUnavailable, classifyUserError(serverFailure(404)))
        assertEquals(UserErrorKind.Fallback, classifyUserError(IllegalStateException("unknown")))
    }

    private fun serverFailure(status: Int) = ServerFailure(null, status)

    private class ServerFailure(
        override val failureCode: String?,
        override val statusCode: Int?,
    ) : IllegalStateException("Server request failed"), CodedFailure {
        override val requestId: String? = null
    }
}
