package dev.typetype.android.data.download

import dev.typetype.android.data.network.ServerError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadFailureCodesTest {
    @Test
    fun recognizesInstanceStorageExhaustion() {
        val code = DownloadFailureCodes.fromHttp(
            status = 507,
            error = ServerError("ignored server text", null),
        )

        assertEquals(DownloadFailureCodes.InsufficientStorage, code)
        assertFalse(DownloadFailureCodes.isRetryable(code))
    }

    @Test
    fun retriesTemporaryServerFailures() {
        val code = DownloadFailureCodes.fromHttp(
            status = 503,
            error = ServerError("ignored server text", "internal_error"),
        )

        assertEquals(DownloadFailureCodes.ServerUnavailable, code)
        assertTrue(DownloadFailureCodes.isRetryable(code))
    }
}
