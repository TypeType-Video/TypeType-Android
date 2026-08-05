package dev.typetype.android.domain.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IncomingVideoUrlTest {

    @Test
    fun compactFrontendIdentifiersBecomeProviderUrls() {
        assertEquals(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            resolveIncomingVideoUrl("dQw4w9WgXcQ"),
        )
        assertEquals(
            "https://www.nicovideo.jp/watch/sm9",
            resolveIncomingVideoUrl("sm9"),
        )
        assertEquals(
            "https://www.bilibili.com/video/BV1xx411c7mD?p=3",
            resolveIncomingVideoUrl("BV1xx411c7mD?p=3"),
        )
    }

    @Test
    fun typeTypeWatchUrlUnwrapsItsSource() {
        assertEquals(
            "https://www.nicovideo.jp/watch/sm9",
            resolveIncomingVideoUrl(
                "https://beta.typetype.video/watch?v=https%3A%2F%2Fwww.nicovideo.jp%2Fwatch%2Fsm9",
            ),
        )
        assertEquals(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            resolveIncomingVideoUrl("https://example.com/watch?v=dQw4w9WgXcQ"),
        )
    }

    @Test
    fun customSchemeUsesTheSameWatchContract() {
        assertEquals(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            resolveIncomingVideoUrl("typetype://watch?v=dQw4w9WgXcQ"),
        )
    }

    @Test
    fun sharedTextFindsTheFirstUsableUrl() {
        assertEquals(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            resolveSharedVideoUrl("Interesting video: https://youtu.be/dQw4w9WgXcQ."),
        )
        assertEquals(
            "https://www.nicovideo.jp/watch/sm9",
            resolveSharedVideoUrl("https://nico.ms/sm9"),
        )
        assertEquals(
            "https://b23.tv/example",
            resolveSharedVideoUrl("https://b23.tv/example"),
        )
    }

    @Test
    fun unsafeOrNonVideoValuesAreRejected() {
        assertNull(resolveIncomingVideoUrl("file:///private/video.mp4"))
        assertNull(resolveIncomingVideoUrl("https://user:password@example.com/video"))
        assertNull(resolveIncomingVideoUrl("https://example.com/video"))
        assertNull(resolveIncomingVideoUrl("https://beta.typetype.video/watch"))
        assertNull(resolveIncomingVideoUrl("not a video"))
        assertNull(resolveSharedVideoUrl("plain text without a link"))
    }
}
