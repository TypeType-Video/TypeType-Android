package dev.typetype.android.data.youtubesession

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class YoutubeRemoteBrowserUrlTest {
    @Test
    fun resolvesTheServerRelativeSocketBelowTheApiBase() {
        val url = resolveYoutubeRemoteBrowserUrl(
            baseUrl = "https://video.example/api/",
            value = "/youtube-session/browser/id?token=one-time",
        )

        assertEquals(
            "https://video.example/api/youtube-session/browser/id?token=one-time",
            url.toString(),
        )
    }

    @Test
    fun acceptsAnAbsoluteSocketOnTheSameOrigin() {
        val url = resolveYoutubeRemoteBrowserUrl(
            baseUrl = "https://video.example/api/",
            value = "wss://video.example/api/youtube-session/browser/id?token=one-time",
        )

        assertEquals("video.example", url.host)
        assertEquals("/api/youtube-session/browser/id", url.encodedPath)
    }

    @Test
    fun rejectsAnotherOriginAndTransportDowngrade() {
        assertThrows(IllegalArgumentException::class.java) {
            resolveYoutubeRemoteBrowserUrl(
                "https://video.example/api/",
                "wss://other.example/youtube-session/browser/id?token=secret",
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            resolveYoutubeRemoteBrowserUrl(
                "https://video.example/api/",
                "ws://video.example/api/youtube-session/browser/id?token=secret",
            )
        }
    }
}
