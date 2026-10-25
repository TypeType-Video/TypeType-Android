package dev.typetype.android.core.error

import kotlin.coroutines.cancellation.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SuspendRunCatchingTest {
    @Test
    fun failuresBecomeResultsButCancellationIsRethrown() {
        assertEquals(2, suspendRunCatching { 1 + 1 }.getOrThrow())
        assertTrue(suspendRunCatching { error("boom") }.isFailure)
        assertThrows(CancellationException::class.java) {
            suspendRunCatching { throw CancellationException("left the screen") }
        }
    }
}
