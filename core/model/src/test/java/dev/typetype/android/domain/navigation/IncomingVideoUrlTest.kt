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
        assertEquals(
            "https://www.bilibili.com/video/BV1xx411c7mD?p=3",
            resolveIncomingVideoUrl(
                "https://watch.example/api/proxy?url=" +
                    "https%3A%2F%2Fwww.bilibili.com%2Fvideo%2FBV1xx411c7mD%3Fp%3D3",
            ),
        )
    }

    @Test
    fun publicWatchParametersMatchTheFrontendContract() {
        assertEquals("dQw4w9WgXcQ", toPublicWatchParameter("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals("sm9", toPublicWatchParameter("https://www.nicovideo.jp/watch/sm9"))
        assertEquals(
            "BV1xx411c7mD?p=3",
            toPublicWatchParameter("https://www.bilibili.com/video/BV1xx411c7mD?p=3"),
        )
        assertEquals(
            "sm9",
            toPublicWatchParameter(
                "https://watch.example/watch?v=" +
                    "https%3A%2F%2Fwww.nicovideo.jp%2Fwatch%2Fsm9",
            ),
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

    @Test
    fun `extracts timestamps from same video urls`() {
        val current = "https://www.youtube.com/watch?v=abc12345678"

        assertEquals(125_000L, sameVideoTimestampMillis("https://youtu.be/abc12345678?t=125", current))
        assertEquals(
            85_000L,
            sameVideoTimestampMillis("https://www.youtube.com/watch?v=abc12345678&t=1m25s", current),
        )
        assertEquals(
            3_723_000L,
            sameVideoTimestampMillis("https://www.youtube.com/watch?v=abc12345678&t=1h2m3s", current),
        )
        assertEquals(30_000L, sameVideoTimestampMillis("https://youtu.be/abc12345678?start=30s", current))
    }

    @Test
    fun `ignores timestamps from other videos`() {
        val current = "https://www.youtube.com/watch?v=abc12345678"

        assertNull(sameVideoTimestampMillis("https://youtu.be/other456789?t=125", current))
    }

    @Test
    fun `ignores same video urls without timestamp`() {
        val current = "https://www.youtube.com/watch?v=abc12345678"

        assertNull(sameVideoTimestampMillis("https://youtu.be/abc12345678", current))
    }
}
