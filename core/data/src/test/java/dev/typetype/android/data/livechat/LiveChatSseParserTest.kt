package dev.typetype.android.data.livechat

import dev.typetype.android.domain.livechat.LiveChatEvent
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class LiveChatSseParserTest {
    private val parser = LiveChatSseParser(Json { ignoreUnknownKeys = true })

    private fun feed(vararg lines: String): List<LiveChatEvent> = lines.mapNotNull(parser::accept)

    @Test
    fun `keep alives are ignored and messages are decoded`() {
        val events = feed(
            ": connected",
            "",
            "event: message",
            """data: {"id":"1","text":"hi","receivedAtMs":5,"authorName":"Ann","moderator":true}""",
            "",
            ": keep-alive",
            "",
        )

        val message = (events.single() as LiveChatEvent.Message).message
        assertEquals("hi", message.text)
        assertEquals("Ann", message.authorName)
        assertEquals(true, message.moderator)
        assertNull(message.authorAvatarUrl)
    }

    @Test
    fun `ended event closes the chat`() {
        assertEquals(listOf(LiveChatEvent.Ended), feed("event: ended", "data: {}", ""))
    }

    @Test
    fun `error event fails the stream with the server reason`() {
        feed("event: error", """data: {"error":"Live chat retrieval failed"}""")

        val failure = assertThrows(LiveChatStreamException::class.java) { parser.accept("") }
        assertEquals("Live chat retrieval failed", failure.message)
    }

    @Test
    fun `malformed messages are skipped`() {
        assertEquals(emptyList<LiveChatEvent>(), feed("data: {\"id\":1}", ""))
        assertNull(parser.finish())
    }
}
