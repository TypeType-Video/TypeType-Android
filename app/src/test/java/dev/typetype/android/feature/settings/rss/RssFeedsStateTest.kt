package dev.typetype.android.feature.settings.rss

import dev.typetype.android.R
import dev.typetype.android.domain.rss.RssFeed
import dev.typetype.android.domain.rss.RssFeedScope
import dev.typetype.android.domain.server.RssCapability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RssFeedsStateTest {
    @Test
    fun creationWaitsForTheServerListAndRespectsItsLimit() {
        val loading = RssFeedsState(capability = RssCapability(enabled = true, maxFeedsPerUser = 1))
        val available = loading.copy(hasLoadedFeeds = true)
        val full = available.copy(feeds = listOf(feed()))

        assertFalse(loading.canCreate)
        assertTrue(available.canCreate)
        assertFalse(full.canCreate)
    }

    @Test
    fun selectedChannelDraftRequiresChannelsAndKeepsTheServerMaximum() {
        val empty = validEditor().copy(scope = RssFeedScope.Channels)
        val maximum = empty.copy(channelUrls = (1..100).map { "channel-$it" }.toSet())
        val tooMany = maximum.copy(channelUrls = maximum.channelUrls + "channel-101")

        assertEquals(R.string.rss_error_channels, empty.validationError())
        assertNull(maximum.validationError())
        assertEquals(R.string.rss_error_channels, tooMany.validationError())
    }

    private fun validEditor() = RssFeedEditorState(name = "Feed", serviceIds = setOf(0))

    private fun feed() = RssFeed(
        id = "feed",
        name = "Feed",
        scope = RssFeedScope.All,
        channelUrls = emptyList(),
        serviceIds = setOf(0),
        includeVideos = true,
        includeShorts = true,
        includeLive = true,
        includeUpcoming = true,
        enabled = true,
        createdAt = 1,
        updatedAt = 2,
        lastUsedAt = null,
    )
}
