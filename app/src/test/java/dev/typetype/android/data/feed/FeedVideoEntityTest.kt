package dev.typetype.android.data.feed

import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.domain.feed.Video
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedVideoEntityTest {
    @Test
    fun roundTripPreservesAvailabilityAndPresentationFields() {
        val video = Video(
            id = "video-id",
            url = "https://example.test/watch?v=video-id",
            title = "Premiere",
            thumbnailUrl = "https://example.test/thumb.jpg",
            uploaderName = "Channel",
            uploaderUrl = "https://example.test/channel",
            uploaderAvatarUrl = "https://example.test/avatar.gif",
            uploaderVerified = true,
            durationSeconds = 123,
            isLive = false,
            viewCount = 456,
            uploadedAtMillis = 789,
            isShortFormContent = false,
            shortDescription = "Description",
            publishedAtMillis = 1_234,
            isPostLive = false,
            isLiveContent = true,
            requiresMembership = true,
        )

        val entity = video.toFeedEntity(
            scope = AccountScope("server", "account"),
            feed = "home",
            position = 2,
            savedAtMillis = 5_678,
        )

        assertEquals(video, entity.toDomainVideo())
        assertEquals(2, entity.position)
        assertEquals(5_678, entity.savedAtMillis)
    }
}
