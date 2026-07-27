package dev.typetype.android.data.stream

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SabrStreamRequestCancellationTest {
    @Test
    fun `cancelled SABR discovery is not exposed as a stream failure`() = runBlocking {
        val cancellation = CancellationException("superseded SABR request")

        val thrown = runCatching {
            cancellableStreamResult<Unit> { throw cancellation }
        }.exceptionOrNull()

        assertSame(cancellation, thrown)
    }

    @Test
    fun `ordinary SABR discovery failure remains a result`() = runBlocking {
        val result = cancellableStreamResult<Unit> { throw IOException("network") }

        assertTrue(result.exceptionOrNull() is IOException)
    }
}
