package dev.typetype.android.core.url

import org.junit.Assert.assertEquals
import org.junit.Test

class BilibiliImageSizeTest {
    private val api = "https://watch.example/api"

    private fun proxied(url: String) = "$api/proxy?url=${percentEncode(url)}"

    @Test
    fun `asks Bilibili for card sized covers and small avatars`() {
        assertEquals(
            proxied("https://i0.hdslb.com/bfs/archive/abc.jpg@672w_378h_1c.webp"),
            buildImageUrl(api, "http://i0.hdslb.com/bfs/archive/abc.jpg"),
        )
        assertEquals(
            proxied("https://i1.hdslb.com/bfs/face/def.jpg@240w_240h_1c.webp"),
            buildImageUrl(api, "https://i1.hdslb.com/bfs/face/def.jpg"),
        )
    }

    @Test
    fun `keeps images that already carry a size and other Bilibili paths`() {
        assertEquals(
            proxied("https://i0.hdslb.com/bfs/archive/abc.jpg@320w_180h.webp"),
            buildImageUrl(api, "https://i0.hdslb.com/bfs/archive/abc.jpg@320w_180h.webp"),
        )
        assertEquals(
            proxied("https://i0.hdslb.com/bfs/emote/smile.png"),
            buildImageUrl(api, "https://i0.hdslb.com/bfs/emote/smile.png"),
        )
    }

    @Test
    fun `serves biliimg and scheme relative covers through the proxy`() {
        assertEquals(
            proxied("https://i0.hdslb.com/bfs/archive/abc.jpg@672w_378h_1c.webp"),
            buildImageUrl(api, "https://archive.biliimg.com/bfs/archive/abc.jpg"),
        )
        assertEquals(
            proxied("https://i2.hdslb.com/bfs/face/def.jpg@240w_240h_1c.webp"),
            buildImageUrl(api, "//i2.hdslb.com/bfs/face/def.jpg"),
        )
    }
}
