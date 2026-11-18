package dev.typetype.android.feature.player.livechat

import dev.typetype.android.domain.twitch.TwitchPage
import dev.typetype.android.domain.twitch.TwitchVideoComment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VodChatWindowTest {
    @Test
    fun theFirstPageStartsWhereTheViewerIsAndShowsOnlyWhatWasSaid() {
        val window = VodChatWindow()
        assertEquals(VodChatRequest.From(100), window.request(100))
        window.accept(VodChatRequest.From(100), page(listOf(100, 110, 140), cursor = "c2"), 100)

        assertEquals(listOf("100"), window.visible(105).map { it.id })
        assertNull(window.request(105))
    }

    @Test
    fun theNextPageComesBeforeTheLoadedOneRunsOut() {
        val window = VodChatWindow()
        window.accept(VodChatRequest.From(100), page(listOf(100, 110, 140), cursor = "c2"), 100)

        assertEquals(VodChatRequest.Next("c2"), window.request(125))
        window.accept(VodChatRequest.Next("c2"), page(listOf(140, 150), cursor = null), 125)
        assertEquals(listOf("100", "110", "140", "150"), window.visible(150).map { it.id })
        assertNull(window.request(150))
    }

    @Test
    fun aSeekOutsideTheLoadedChatStartsAgainFromThere() {
        val window = VodChatWindow()
        window.accept(VodChatRequest.From(100), page(listOf(100, 110), cursor = "c2"), 100)

        assertEquals(VodChatRequest.From(500), window.request(500))
        assertEquals(VodChatRequest.From(50), window.request(50))
    }

    private fun page(offsets: List<Long>, cursor: String?) = TwitchPage(
        offsets.map { TwitchVideoComment(it.toString(), it, "viewer", "hi", moderator = false) },
        cursor,
    )
}
