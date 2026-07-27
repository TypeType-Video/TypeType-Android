package dev.typetype.android.data.library

import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.network.dto.HistoryItemDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryResponseMapperTest {
    @Test
    fun `blank server id means history tracking rejected the write`() {
        assertNull(history(id = "").toPostedHistoryEntity(SCOPE))
    }

    @Test
    fun `persisted server history keeps its authoritative id`() {
        val entity = history(id = "history-42").toPostedHistoryEntity(SCOPE)

        assertEquals("history-42", entity?.id)
        assertEquals("youtube-video", entity?.url)
    }

    @Test
    fun `history page advances by received server rows`() {
        val page = HistoryPage(emptyList(), offset = 60, receivedCount = 60, totalCount = 125)

        assertEquals(120, page.nextOffset)
        assertEquals(true, page.hasMore)
    }

    @Test
    fun `empty history page stops pagination`() {
        val page = HistoryPage(emptyList(), offset = 60, receivedCount = 0, totalCount = 125)

        assertEquals(false, page.hasMore)
    }

    private fun history(id: String) = HistoryItemDto(
        id = id,
        url = "youtube-video",
        title = "Video",
        thumbnail = "thumbnail",
        channelName = "Channel",
        channelUrl = "channel",
        duration = 120L,
        progress = 10L,
        watchedAt = 20L,
    )

    private companion object {
        val SCOPE = AccountScope("server", "account")
    }
}
