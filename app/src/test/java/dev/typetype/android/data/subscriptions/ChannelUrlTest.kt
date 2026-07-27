package dev.typetype.android.data.subscriptions

import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelUrlTest {
    @Test
    fun normalizesWebChannelIdentityWithoutKeepingTrackingData() {
        assertEquals(
            "https://youtube.com/channel/UC123",
            normalizeChannelUrl(" http://YouTube.com/channel/UC123/?source=app#top "),
        )
    }

    @Test
    fun leavesNonWebProviderIdentityStable() {
        assertEquals("provider:channel:42", normalizeChannelUrl("provider:channel:42/"))
    }
}
