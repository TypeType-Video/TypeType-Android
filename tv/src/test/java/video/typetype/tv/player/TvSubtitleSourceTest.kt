package video.typetype.tv.player

import org.junit.Assert.assertEquals
import org.junit.Test
import video.typetype.sdk.core.TypeTypeError

class TvSubtitleSourceTest {
    @Test
    fun httpFailureKeepsServerCodeAndRequestId() {
        val error = TypeTypeError.Http(
            status = 429,
            code = "subtitle_upstream_throttled",
            message = "upstream throttled",
            requestId = "request-1",
        )

        assertEquals(
            "Server error 429 (subtitle_upstream_throttled) · request request-1",
            error.toSubtitleMessage(),
        )
    }

    @Test
    fun networkFailureKeepsTheActionableMessage() {
        assertEquals(
            "Subtitle request timed out",
            TypeTypeError.Network("Subtitle request timed out", null).toSubtitleMessage(),
        )
    }
}
