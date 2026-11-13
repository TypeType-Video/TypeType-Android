package dev.typetype.android.feature.menu

import dev.typetype.android.domain.feed.Video
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoMenuScopeProgressTest {
    @Test
    fun `server position becomes the share of the video already watched`() {
        val scope = scope(mapOf("https://www.youtube.com/watch?v=aqz-KE-bpKQ" to 300_000L))

        assertEquals(0.5f, scope.stateFor(video("https://youtu.be/aqz-KE-bpKQ", 600)).progressFraction, 0.001f)
    }

    @Test
    fun `unknown position or duration shows no progress`() {
        val scope = scope(mapOf("https://www.youtube.com/watch?v=aqz-KE-bpKQ" to 300_000L))

        assertEquals(0f, scope.stateFor(video("https://www.youtube.com/watch?v=other", 600)).progressFraction)
        assertEquals(0f, scope.stateFor(video("https://www.youtube.com/watch?v=aqz-KE-bpKQ", 0)).progressFraction)
    }

    @Test
    fun `progress never goes past the end`() {
        val scope = scope(mapOf("https://www.youtube.com/watch?v=aqz-KE-bpKQ" to 900_000L))

        assertTrue(scope.stateFor(video("https://www.youtube.com/watch?v=aqz-KE-bpKQ", 600)).progressFraction <= 1f)
    }

    private fun scope(positions: Map<String, Long>) = VideoMenuScope(
        watchedUrls = emptySet(),
        blockedVideoUrls = emptySet(),
        blockedChannelUrls = emptySet(),
        blockedKeywords = emptySet(),
        positions = positions,
        favorites = emptySet(),
        watchLater = emptySet(),
        onAction = { _, _ -> },
    )

    private fun video(url: String, durationSeconds: Long) = Video(
        id = url,
        url = url,
        title = "Video",
        thumbnailUrl = "",
        uploaderName = "",
        uploaderUrl = "",
        uploaderAvatarUrl = "",
        uploaderVerified = false,
        durationSeconds = durationSeconds,
        isLive = false,
        viewCount = 0,
        uploadedAtMillis = 0,
        isShortFormContent = false,
        shortDescription = null,
    )
}
