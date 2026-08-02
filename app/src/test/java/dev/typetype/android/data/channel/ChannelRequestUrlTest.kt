package dev.typetype.android.data.channel

import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelRequestUrlTest {
    @Test
    fun regularChannelRequestKeepsSourceUrl() {
        val url = "https://www.youtube.com/@TypeType"

        assertEquals(url, buildChannelRequestUrl(url, searchQuery = "", live = false))
    }

    @Test
    fun searchRequestUsesYoutubeChannelSearchPath() {
        val result = buildChannelRequestUrl(
            channelUrl = "https://www.youtube.com/@TypeType/",
            searchQuery = "  android player  ",
            live = false,
        )

        assertEquals(
            "https://www.youtube.com/@TypeType/search?query=android%20player",
            result,
        )
    }

    @Test
    fun liveRequestUsesStreamsPathWithoutSearchQuery() {
        val result = buildChannelRequestUrl(
            channelUrl = "https://youtube.com/channel/example?view=0",
            searchQuery = "ignored",
            live = true,
        )

        assertEquals("https://youtube.com/channel/example/streams", result)
    }

    @Test
    fun existingSearchUrlIsSplitBeforeBuildingAnotherRequest() {
        val source = "https://youtube.com/@TypeType/search?query=old"

        assertEquals(
            "https://youtube.com/@TypeType" to "old",
            splitChannelSearchUrl(source),
        )
        assertEquals(
            "https://youtube.com/@TypeType/search?query=new",
            buildChannelRequestUrl(source, "new", live = false),
        )
    }

    @Test
    fun unsupportedProviderDoesNotReceiveYoutubePaths() {
        val url = "https://video.example/channel/typetype"

        assertEquals(url, buildChannelRequestUrl(url, "query", live = false))
        assertEquals(url, buildChannelRequestUrl(url, "", live = true))
    }
}
