package dev.typetype.android.data.library.sync

import dev.typetype.android.data.network.ServerError
import dev.typetype.android.data.network.ServerResponseException
import java.io.IOException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressSyncPolicyTest {
    @Test
    fun `retries transport throttling and temporary server failures`() {
        assertTrue(shouldRetryProgressSync(IOException("offline")))
        assertTrue(shouldRetryProgressSync(serverFailure(408)))
        assertTrue(shouldRetryProgressSync(serverFailure(429)))
        assertTrue(shouldRetryProgressSync(serverFailure(503)))
    }

    @Test
    fun `does not retry permanent or unexpected failures`() {
        assertFalse(shouldRetryProgressSync(serverFailure(400)))
        assertFalse(shouldRetryProgressSync(serverFailure(403)))
        assertFalse(shouldRetryProgressSync(IllegalStateException("invalid payload")))
    }

    @Test
    fun `accepts only the captured session generation and instance address`() {
        assertTrue(isProgressSyncTargetCurrent(7L, "https://one/api/", 7L, "https://one/api/"))
        assertFalse(isProgressSyncTargetCurrent(8L, "https://one/api/", 7L, "https://one/api/"))
        assertFalse(isProgressSyncTargetCurrent(7L, "https://two/api/", 7L, "https://one/api/"))
    }

    @Test
    fun `rejects missing state and invalid captured generations`() {
        assertFalse(isProgressSyncTargetCurrent(null, "https://one/api/", 7L, "https://one/api/"))
        assertFalse(isProgressSyncTargetCurrent(7L, null, 7L, "https://one/api/"))
        assertFalse(isProgressSyncTargetCurrent(7L, "https://one/api/", -1L, "https://one/api/"))
    }

    private fun serverFailure(status: Int) = ServerResponseException(
        ServerError("Server request failed", null, status),
    )
}
