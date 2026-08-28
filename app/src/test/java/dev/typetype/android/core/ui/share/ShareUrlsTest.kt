package dev.typetype.android.core.ui.share

import org.junit.Assert.assertEquals
import org.junit.Test

class ShareUrlsTest {

    @Test
    fun sharesCompactPublicWatchRoutes() {
        assertEquals(
            "https://watch.example/watch?v=dQw4w9WgXcQ",
            buildShareUrl("https://watch.example/api", "https://youtube.com/watch?v=dQw4w9WgXcQ"),
        )
        assertEquals(
            "https://watch.example/watch?v=sm9",
            buildShareUrl("https://watch.example/api/", "https://nicovideo.jp/watch/sm9"),
        )
        assertEquals(
            "https://watch.example/watch?v=BV1xx411c7mD%3Fp%3D3",
            buildShareUrl(
                "https://watch.example/api",
                "https://bilibili.com/video/BV1xx411c7mD?p=3",
            ),
        )
    }

    @Test
    fun preservesSourceWhenNoServerIsActive() {
        assertEquals(
            "https://youtube.com/watch?v=dQw4w9WgXcQ",
            buildShareUrl(null, "https://youtube.com/watch?v=dQw4w9WgXcQ"),
        )
    }

    @Test
    fun proxiesRemoteImagesThroughTheActiveServer() {
        assertEquals(
            "https://watch.example/api/proxy?url=https%3A%2F%2Fi.ytimg.com%2Fvi%2Fabc%2Fhqdefault.jpg",
            buildImageUrl(
                "https://watch.example/api",
                "https://i.ytimg.com/vi/abc/hqdefault.jpg",
            ),
        )
    }

    @Test
    fun resolvesServerRelativeImagesAndKeepsServerImagesUnchanged() {
        assertEquals(
            "https://watch.example/api/images/avatar.jpg",
            buildImageUrl(
                "https://watch.example/api",
                "https://watch.example/api/images/avatar.jpg",
            ),
        )
        assertEquals(
            "https://watch.example/images/avatar.jpg",
            buildImageUrl("https://watch.example/api", "/images/avatar.jpg"),
        )
    }

    @Test
    fun leavesNonMediaRemoteImagesDirect() {
        assertEquals(
            "https://cdn.example/avatar.jpg",
            buildImageUrl("https://watch.example/api", "https://cdn.example/avatar.jpg"),
        )
    }
}
