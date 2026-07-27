package dev.typetype.android.core.ui.error

import dev.typetype.android.data.network.ServerError
import dev.typetype.android.data.network.ServerResponseException
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class UserErrorKindTest {
    @Test
    fun `classifies transport authentication and permission failures`() {
        assertEquals(UserErrorKind.NetworkUnavailable, classifyUserError(IOException("offline")))
        assertEquals(UserErrorKind.SignInAgain, classifyUserError(serverFailure(401)))
        assertEquals(UserErrorKind.PermissionDenied, classifyUserError(serverFailure(403)))
    }

    @Test
    fun `classifies conflict throttling and temporary server failures`() {
        assertEquals(UserErrorKind.Conflict, classifyUserError(serverFailure(409)))
        assertEquals(UserErrorKind.RateLimited, classifyUserError(serverFailure(429)))
        assertEquals(UserErrorKind.ServerUnavailable, classifyUserError(serverFailure(503)))
    }

    @Test
    fun `stable server code wins over an unusual status`() {
        val failure = ServerResponseException(
            ServerError("Too many requests", "rate_limited", 418),
        )

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

    private fun serverFailure(status: Int) = ServerResponseException(
        ServerError("Server request failed", null, status),
    )
}
