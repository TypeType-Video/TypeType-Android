package dev.typetype.android.core.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelNavigationUrlTest {
    @Test
    fun expandsYoutubeChannelId() {
        assertEquals(
            "https://www.youtube.com/channel/UCMiJRAwDNSNzuYeN2uWa0pA",
            channelNavigationUrl(" UCMiJRAwDNSNzuYeN2uWa0pA "),
        )
    }

    @Test
    fun expandsYoutubeHandleAndRelativePaths() {
        assertEquals("https://www.youtube.com/@TypeType", channelNavigationUrl("@TypeType"))
        assertEquals(
            "https://www.youtube.com/channel/UCMiJRAwDNSNzuYeN2uWa0pA",
            channelNavigationUrl("/channel/UCMiJRAwDNSNzuYeN2uWa0pA"),
        )
    }

    @Test
    fun keepsCompleteProviderUrl() {
        val url = "https://video.example/channel/typetype?view=videos"
        assertEquals(url, channelNavigationUrl(url))
    }
}
