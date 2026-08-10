package dev.typetype.android.feature.player

import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamPlaybackContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerMetadataPrefetchCacheTest {
    private val cache = PlayerMetadataPrefetchCache()

    @Test
    fun `metadata remains available for revisiting its exact video URL`() {
        cache.put("https://video/next", stream("next"))

        assertNull(cache.get("https://video/other"))
        assertEquals("next", cache.get("https://video/next")?.id)
        assertEquals("next", cache.get("https://video/next")?.id)
    }

    @Test
    fun `adjacent prefetches remain available independently`() {
        cache.put("https://video/one", stream("one"))
        cache.put("https://video/two", stream("two"))

        assertEquals("one", cache.get("https://video/one")?.id)
        assertEquals("two", cache.get("https://video/two")?.id)
    }

    @Test
    fun `oldest prefetch is evicted when the cache is full`() {
        listOf("one", "two", "three", "four").forEach {
            cache.put("https://video/$it", stream(it))
        }

        assertNull(cache.get("https://video/one"))
        assertEquals("two", cache.get("https://video/two")?.id)
        assertEquals("three", cache.get("https://video/three")?.id)
        assertEquals("four", cache.get("https://video/four")?.id)
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
