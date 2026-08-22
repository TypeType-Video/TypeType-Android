package dev.typetype.android.data.stream

import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamPlaybackContract
import dev.typetype.android.domain.stream.StreamRequestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackStreamPrefetchCacheTest {
    private var now = 1_000L
    private val cache = PlaybackStreamPrefetchCache(nowMillis = { now }, ttlMillis = 500L)

    @Test
    fun `prefetch is isolated by video instance and account`() {
        val firstScope = scope(accountId = "first")
        cache.put("video-one", stream("one", firstScope))

        assertEquals("one", cache.get("video-one", firstScope)?.id)
        assertNull(cache.get("video-two", firstScope))
        assertNull(cache.get("video-one", scope(accountId = "second")))
        assertNull(cache.get("video-one", firstScope.copy(baseUrl = "https://other.example/api/")))
    }

    @Test
    fun `expired playback bootstrap is not reused`() {
        val scope = scope()
        cache.put("video", stream("video", scope))

        now += 500L

        assertNull(cache.get("video", scope))
    }

    @Test
    fun `only adjacent playback bootstraps remain cached`() {
        val scope = scope()
        listOf("one", "two", "three", "four").forEach { id ->
            cache.put(id, stream(id, scope))
        }

        assertNull(cache.get("one", scope))
        assertEquals("two", cache.get("two", scope)?.id)
        assertEquals("three", cache.get("three", scope)?.id)
        assertEquals("four", cache.get("four", scope)?.id)
    }

    private fun scope(accountId: String = "account") = StreamRequestScope(
        serverId = "server",
        accountId = accountId,
        baseUrl = "https://instance.example/api/",
    )

    private fun stream(id: String, scope: StreamRequestScope) = Stream(
        playbackContract = StreamPlaybackContract.ServerSabr,
        id = id,
        title = id,
        uploaderName = "Channel",
        uploaderAvatarUrl = "",
        uploaderUrl = "",
        uploaderSubscriberCount = -1L,
        uploaderVerified = false,
        thumbnailUrl = "",
        description = "",
        durationSeconds = 30L,
        viewCount = -1L,
        likeCount = -1L,
        dislikeCount = -1L,
        uploadedAtMillis = -1L,
        hlsUrl = null,
        dashMpdUrl = null,
        progressiveUrl = null,
        serverDashManifestUrl = null,
        serverHlsManifestUrl = null,
        requestScope = scope,
        startPositionMillis = 0L,
    )
}
