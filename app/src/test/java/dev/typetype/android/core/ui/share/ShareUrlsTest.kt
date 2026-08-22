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
}
