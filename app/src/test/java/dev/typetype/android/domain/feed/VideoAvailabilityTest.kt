package dev.typetype.android.domain.feed

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoAvailabilityTest {
    @Test
    fun `future publication is a scheduled premiere`() {
        val video = video(publishedAtMillis = NOW + 60_000L)

        assertEquals(VideoAvailability.Scheduled, video.availabilityAt(NOW))
    }

    @Test
    fun `membership takes priority over publication time`() {
        val video = video(
            publishedAtMillis = NOW + 60_000L,
            requiresMembership = true,
        )

        assertEquals(VideoAvailability.MembersOnly, video.availabilityAt(NOW))
    }

    @Test
    fun `published video remains playable`() {
        val video = video(publishedAtMillis = NOW - 60_000L)

        assertEquals(VideoAvailability.Playable, video.availabilityAt(NOW))
    }

    @Test
    fun `thumbnail statuses distinguish live replay premiere and upcoming`() {
        assertEquals(VideoBadgeStatus.Live, video(null, isLive = true).badgeStatusAt(NOW))
        assertEquals(VideoBadgeStatus.Replay, video(null, isPostLive = true).badgeStatusAt(NOW))
        assertEquals(
            VideoBadgeStatus.Premiere,
            video(NOW + 60_000L, isLiveContent = true).badgeStatusAt(NOW),
        )
        assertEquals(
            VideoBadgeStatus.Upcoming,
            video(null, isLiveContent = true).badgeStatusAt(NOW),
        )
    }

    private fun video(
        publishedAtMillis: Long?,
        requiresMembership: Boolean = false,
        isLive: Boolean = false,
        isPostLive: Boolean = false,
        isLiveContent: Boolean = false,
    ) = Video(
        id = "video",
        url = "https://www.youtube.com/watch?v=video",
        title = "Video",
        thumbnailUrl = "",
        uploaderName = "Channel",
        uploaderUrl = "",
        uploaderAvatarUrl = "",
        uploaderVerified = false,
        durationSeconds = 60L,
        isLive = isLive,
        viewCount = 1L,
        uploadedAtMillis = publishedAtMillis ?: -1L,
        isShortFormContent = false,
        shortDescription = null,
        publishedAtMillis = publishedAtMillis,
        isPostLive = isPostLive,
        isLiveContent = isLiveContent,
        requiresMembership = requiresMembership,
    )

    private companion object {
        const val NOW = 1_000_000L
    }
}
