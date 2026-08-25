package dev.typetype.android.feature.settings.blocked

import org.junit.Assert.assertEquals
import org.junit.Test

class BlockedItemDisplayPathTest {
    @Test
    fun youtubeWatchUrlShowsOnlyTheVideoIdentity() {
        assertEquals(
            "/xcbadhbca",
            blockedItemDisplayPath("https://www.youtube.com/watch?v=xcbadhbca"),
        )
    }

    @Test
    fun channelUrlKeepsTheUsefulPathWithoutTheDomain() {
        assertEquals(
            "/channel/UC123",
            blockedItemDisplayPath("https://www.youtube.com/channel/UC123"),
        )
    }

    @Test
    fun relativePathsStayUnchanged() {
        assertEquals("/@creator", blockedItemDisplayPath("/@creator"))
    }
}
