package dev.typetype.android.data.library.sync

import dev.typetype.android.data.network.ServerError
import dev.typetype.android.data.network.ServerResponseException
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibrarySyncFailureTest {
    @Test
    fun `keeps only structured server failure metadata`() {
        val snapshot = syncFailureSnapshot(
            ServerResponseException(
                ServerError(
                    message = "private server detail",
                    code = "rate_limited",
                    statusCode = 429,
                    requestId = "req-123",
                ),
            ),
        )

        assertEquals("rate_limited", snapshot.code)
        assertEquals(429, snapshot.statusCode)
        assertEquals("req-123", snapshot.requestId)
    }

    @Test
    fun `maps transport failure without retaining its message`() {
        val snapshot = syncFailureSnapshot(IOException("private host failed"))

        assertEquals("client_network_unavailable", snapshot.code)
        assertNull(snapshot.statusCode)
        assertNull(snapshot.requestId)
    }

    @Test
    fun `rejects malformed code and request id`() {
        val snapshot = syncFailureSnapshot(
            ServerResponseException(
                ServerError(
                    message = "ignored",
                    code = "bad code with spaces",
                    statusCode = 500,
                    requestId = "bad request id",
                ),
            ),
        )

        assertNull(snapshot.code)
        assertEquals(500, snapshot.statusCode)
        assertNull(snapshot.requestId)
    }
}
