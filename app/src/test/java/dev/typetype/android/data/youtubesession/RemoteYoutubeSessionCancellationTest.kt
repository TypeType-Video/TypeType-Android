package dev.typetype.android.data.youtubesession

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteYoutubeSessionCancellationTest {
    @Test
    fun `missing browser session means cancellation is complete`() {
        assertTrue(isRemoteBrowserCancellationComplete(204))
        assertTrue(isRemoteBrowserCancellationComplete(404))
    }

    @Test
    fun `other failures remain visible`() {
        assertFalse(isRemoteBrowserCancellationComplete(401))
        assertFalse(isRemoteBrowserCancellationComplete(409))
        assertFalse(isRemoteBrowserCancellationComplete(429))
        assertFalse(isRemoteBrowserCancellationComplete(500))
    }
}
