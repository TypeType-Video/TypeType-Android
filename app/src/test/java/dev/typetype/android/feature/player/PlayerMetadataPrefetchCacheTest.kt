package dev.typetype.android.feature.player

import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamPlaybackContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerMetadataPrefetchCacheTest {
    private val cache = PlayerMetadataPrefetchCache()

    @Test
    fun `metadata is consumed once by its exact video URL`() {
        cache.put("https://video/next", stream("next"))

        assertNull(cache.take("https://video/other"))
        assertEquals("next", cache.take("https://video/next")?.id)
        assertNull(cache.take("https://video/next"))
    }

    @Test
    fun `new prefetch replaces the previous candidate`() {
        cache.put("https://video/one", stream("one"))
        cache.put("https://video/two", stream("two"))

        assertNull(cache.take("https://video/one"))
        assertEquals("two", cache.take("https://video/two")?.id)
    }

    private fun stream(id: String) = Stream(
        playbackContract = StreamPlaybackContract.ServerSabr,
        id = id,
        title = id,
        uploaderName = "",
        uploaderAvatarUrl = "",
        uploaderUrl = "",
        uploaderSubscriberCount = -1,
        uploaderVerified = false,
        thumbnailUrl = "",
        description = "",
        durationSeconds = 1,
        viewCount = -1,
        likeCount = -1,
        dislikeCount = -1,
        uploadedAtMillis = -1,
        hlsUrl = null,
        dashMpdUrl = null,
        progressiveUrl = null,
        serverDashManifestUrl = null,
        serverHlsManifestUrl = null,
        startPositionMillis = 0,
    )
}
