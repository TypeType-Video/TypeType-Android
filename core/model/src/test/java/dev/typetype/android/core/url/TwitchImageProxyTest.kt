package dev.typetype.android.core.url

import org.junit.Assert.assertEquals
import org.junit.Test

class TwitchImageProxyTest {
    @Test
    fun twitchImagesAndEmoteCdnsGoThroughTheInstanceProxy() {
        val base = "https://tube.example/api"
        val emote = "https://cdn.7tv.app/emote/abc/2x.webp"
        val avatar = "https://static-cdn.jtvnw.net/jtv_user_pictures/a.png"

        assertEquals("$base/proxy?url=https%3A%2F%2Fcdn.7tv.app%2Femote%2Fabc%2F2x.webp", buildImageUrl(base, emote))
        assertEquals(true, buildImageUrl(base, avatar).startsWith("$base/proxy?url="))
        assertEquals("https://other.example/a.png", buildImageUrl(base, "https://other.example/a.png"))
    }
}
