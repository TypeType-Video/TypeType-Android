package dev.typetype.android.core.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelAvatarTest {
    @Test
    fun `first attempt uses the original url`() {
        assertEquals(
            "https://example.test/avatar.png",
            avatarRetryUrl("https://example.test/avatar.png", 0),
        )
    }

    @Test
    fun `retries append a distinct cache key`() {
        assertEquals(
            "https://example.test/avatar.png?_tt_avatar_retry=1",
            avatarRetryUrl("https://example.test/avatar.png", 1),
        )
        assertEquals(
            "https://example.test/avatar.png?_tt_avatar_retry=2",
            avatarRetryUrl("https://example.test/avatar.png", 2),
        )
    }

    @Test
    fun `retries keep existing query parameters`() {
        assertEquals(
            "https://example.test/proxy?url=abc&_tt_avatar_retry=1",
            avatarRetryUrl("https://example.test/proxy?url=abc", 1),
        )
    }

    @Test
    fun `blank urls are left untouched`() {
        assertEquals("", avatarRetryUrl("", 2))
        assertEquals("  ", avatarRetryUrl("  ", 2))
    }

    @Test
    fun `initial falls back to a question mark`() {
        assertEquals("C", avatarInitial("CodeYure"))
        assertEquals("?", avatarInitial("   "))
    }
}
