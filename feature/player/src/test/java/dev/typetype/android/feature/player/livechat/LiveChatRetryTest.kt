package dev.typetype.android.feature.player.livechat

import dev.typetype.android.domain.livechat.LiveChatRequestException
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveChatRetryTest {
    @Test
    fun `network drops back off up to fifteen seconds then give up`() {
        val drop = IOException("reset")
        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L, 15_000L), (0..4).map { liveChatRetryDelayMillis(drop, it) })
        assertNull(liveChatRetryDelayMillis(drop, 5))
    }

    @Test
    fun `client errors stop at once except timeouts and capacity`() {
        assertNull(liveChatRetryDelayMillis(LiveChatRequestException(422, "unsupported"), 0))
        assertEquals(1_000L, liveChatRetryDelayMillis(LiveChatRequestException(429, "full"), 0))
        assertEquals(1_000L, liveChatRetryDelayMillis(LiveChatRequestException(408, "slow"), 0))
        assertEquals(2_000L, liveChatRetryDelayMillis(LiveChatRequestException(502, "upstream"), 1))
    }
}
