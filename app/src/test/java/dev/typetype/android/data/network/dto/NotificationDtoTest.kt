package dev.typetype.android.data.network.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NotificationDtoTest {
    @Test
    fun mapsServerPageAndNumericContinuation() {
        val page = NotificationsResponseDto(
            items = listOf(notification()),
            unreadCount = 4,
            nextpage = "3",
        ).toDomain()

        assertEquals(4, page.unreadCount)
        assertEquals(3, page.nextPage)
        assertEquals("video", page.items.single().video.id)
        assertEquals(123L, page.items.single().publishedAtMillis)
    }

    @Test
    fun rejectsMalformedServerContinuation() {
        val response = NotificationsResponseDto(
            items = emptyList(),
            unreadCount = 0,
            nextpage = "opaque",
        )

        assertThrows(IllegalArgumentException::class.java) {
            response.toDomain()
        }
    }

    @Test
    fun clampsNegativeUnreadCount() {
        val page = NotificationsResponseDto(
            items = emptyList(),
            unreadCount = -4,
        ).toDomain()

        assertEquals(0, page.unreadCount)
    }

    private fun notification() = NotificationItemDto(
        type = "subscription_new_video",
        title = "Channel uploaded a new video",
        createdAt = 122L,
        publishedAt = 123L,
        channelUrl = "channel",
        channelName = "Channel",
        channelAvatarUrl = "avatar",
        video = VideoItem(
            id = "video",
            title = "Title",
            url = "video-url",
            thumbnailUrl = "thumbnail",
            uploaderName = "Channel",
            uploaderUrl = "channel",
            uploaderAvatarUrl = "avatar",
            duration = 42L,
            viewCount = 7L,
            uploadDate = "",
            uploaded = 123L,
            streamType = "VIDEO_STREAM",
            isShortFormContent = false,
            uploaderVerified = false,
        ),
    )
}
