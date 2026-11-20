package dev.typetype.android.domain.livechat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TwitchChatPartsTest {
    @Test
    fun nativeEmoteRangesCountCodePointsNotUtf16Units() {
        val parts = TwitchChatParts.parts("😀 Kappa hi", listOf(ChatEmoteRange("25", 2, 6)))

        assertEquals(ChatPart.Text("😀 "), parts[0])
        assertEquals("Kappa", (parts[1] as ChatPart.Emote).image.name)
        assertEquals("https://static-cdn.jtvnw.net/emoticons/v2/25/default/dark/2.0", (parts[1] as ChatPart.Emote).image.url)
        assertEquals(ChatPart.Text(" hi"), parts[2])
    }

    @Test
    fun thirdPartyEmotesMatchWholeWordsAndZeroWidthOnesStackOnTheEmoteBefore() {
        val emotes = mapOf(
            "catJAM" to ChatEmote("catJAM", "https://cdn/cat"),
            "RainTime" to ChatEmote("RainTime", "https://cdn/rain", zeroWidth = true),
        )

        val parts = TwitchChatParts.parts("catJAM RainTime catJAMs", emotes = emotes)

        val emote = parts[0] as ChatPart.Emote
        assertEquals(listOf(ChatImage("RainTime", "https://cdn/rain")), emote.overlays)
        assertEquals(ChatPart.Text(" catJAMs"), parts[1])
    }

    @Test
    fun mentionsAndLinksAreTheirOwnParts() {
        val parts = TwitchChatParts.parts("hey @Toasty, see typetype.tv!", selfLogin = "toasty")

        assertEquals(ChatPart.Mention("@Toasty", self = true), parts[1])
        assertEquals(ChatPart.Link("typetype.tv", "https://typetype.tv"), parts[3])
        assertEquals(ChatPart.Text("!"), parts[4])
    }

    @Test
    fun bareWordsWithUnknownEndingsAreNotLinks() {
        assertNull(TwitchChatParts.linkHref("file.txt"))
        assertEquals("https://example.com/a", TwitchChatParts.linkHref("https://example.com/a"))
    }

    @Test
    fun darkNamesAreLightenedOnADarkThemeAndKeptOnALightOne() {
        val navy = TwitchChatColor.name("#000080", "viewer", dark = true)
        val red = TwitchChatColor.name("#FF0000", "viewer", dark = false)

        assertEquals(true, (navy and 0xFF) > 0x80)
        assertEquals(0xFFFF0000.toInt(), red)
    }
}
