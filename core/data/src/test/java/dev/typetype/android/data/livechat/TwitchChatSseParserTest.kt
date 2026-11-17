package dev.typetype.android.data.livechat

import dev.typetype.android.domain.livechat.LiveChatEvent
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TwitchChatSseParserTest {
    private val parser = TwitchChatSseParser(Json { ignoreUnknownKeys = true })

    private fun feed(json: String): LiveChatEvent? {
        assertNull(parser.accept("event: message"))
        assertNull(parser.accept("data: $json"))
        return parser.accept("")
    }

    @Test
    fun aMessageKeepsItsAuthorAndModeratorBadge() {
        val event = feed("""{"type":"message","id":"m1","login":"mod","displayName":"Mod","text":"be nice","badges":["moderator/1"],"time":"t"}""")
        val message = (event as LiveChatEvent.Message).message
        assertEquals("m1", message.id)
        assertEquals("Mod", message.authorName)
        assertTrue(message.moderator)
    }

    @Test
    fun moderationAndRoomEventsAreRead() {
        assertEquals(LiveChatEvent.Removed("m1"), feed("""{"type":"delete","targetId":"m1","time":"t"}"""))
        assertEquals(LiveChatEvent.Cleared("viewer"), feed("""{"type":"clear","login":"viewer","time":"t"}"""))
        assertEquals(LiveChatEvent.Cleared(null), feed("""{"type":"clear","time":"t"}"""))
        assertNull(feed("""{"type":"room","room":{"slow":3},"time":"t"}"""))
    }

    @Test
    fun keepAlivesAndBrokenDataAreIgnored() {
        assertNull(parser.accept(": keep-alive"))
        assertNull(parser.accept(""))
        assertNull(feed("not json"))
    }
}
