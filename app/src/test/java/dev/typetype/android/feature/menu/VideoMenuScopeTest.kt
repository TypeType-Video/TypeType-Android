package dev.typetype.android.feature.menu

import dev.typetype.android.domain.feed.Video
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoMenuScopeTest {
    @Test
    fun `wrapped video uses canonical favorite and watch later state`() {
        val source = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val scope = VideoMenuScope(
            watchedUrls = emptySet(),
            blockedVideoUrls = emptySet(),
            blockedChannelUrls = emptySet(),
            blockedKeywords = emptySet(),
            favorites = setOf(source),
            watchLater = setOf(source),
            onAction = { _, _ -> },
        )
        val wrapped = video(
            "https://beta.typetype.video/api/proxy?url=" +
                "https%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3DdQw4w9WgXcQ",
        )

        assertTrue(scope.stateFor(wrapped).isFavorite)
        assertTrue(scope.stateFor(wrapped).isInWatchLater)
        assertFalse(scope.stateFor(wrapped).isWatched)
    }

    private fun video(url: String) = Video(
        id = "dQw4w9WgXcQ",
        url = url,
        title = "Video",
        thumbnailUrl = "",
        uploaderName = "Channel",
        uploaderUrl = "https://channel",
        uploaderAvatarUrl = "",
        uploaderVerified = false,
        durationSeconds = 60,
        isLive = false,
        viewCount = 1,
        uploadedAtMillis = 1,
        isShortFormContent = false,
        shortDescription = null,
    )
}
