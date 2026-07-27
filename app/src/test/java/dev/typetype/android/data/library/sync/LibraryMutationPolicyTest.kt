package dev.typetype.android.data.library.sync

import dev.typetype.android.data.network.ServerError
import dev.typetype.android.data.network.ServerResponseException
import java.io.IOException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryMutationPolicyTest {
    @Test
    fun retriesOnlyFailuresThatCanRecoverWithoutUserAction() {
        assertTrue(shouldRetryLibraryMutation(IOException("offline")))
        assertTrue(shouldRetryLibraryMutation(serverFailure(429)))
        assertTrue(shouldRetryLibraryMutation(serverFailure(503)))
        assertFalse(shouldRetryLibraryMutation(serverFailure(401)))
        assertFalse(shouldRetryLibraryMutation(serverFailure(403)))
        assertFalse(shouldRetryLibraryMutation(serverFailure(409)))
    }

    @Test
    fun mutationKeysDoNotConfuseParentAndTargetBoundaries() {
        val first = libraryMutationKey(LibraryMutationKind.PlaylistVideo, "ab", "c")
        val second = libraryMutationKey(LibraryMutationKind.PlaylistVideo, "a", "bc")

        assertNotEquals(first, second)
    }

    private fun serverFailure(status: Int) = ServerResponseException(
        ServerError(message = "Server request failed", code = null, statusCode = status),
    )
}
