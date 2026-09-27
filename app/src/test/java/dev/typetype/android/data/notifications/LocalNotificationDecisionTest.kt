package dev.typetype.android.data.notifications

import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.notifications.NotificationItem
import dev.typetype.android.domain.push.ChannelNotificationsPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNotificationDecisionTest {
    @Test
    fun `first poll records the backlog without notifying`() {
        val decision = decideLocalNotifications(
            items = listOf(item(videoId = "a", channelUrl = CHANNEL), item(videoId = "b", channelUrl = CHANNEL)),
            knownKeys = emptySet(),
            seeded = false,
            enabledChannels = listOf(ChannelNotificationsPreference(CHANNEL, enabled = true)),
        )

        assertEquals(listOf("a", "b"), decision.keysToRecord)
        assertTrue(decision.notified.isEmpty())
    }

    @Test
    fun `alerts only new videos from enabled channels`() {
        val decision = decideLocalNotifications(
            items = listOf(
                item(videoId = "new-enabled", channelUrl = CHANNEL),
                item(videoId = "new-silent", channelUrl = OTHER_CHANNEL),
                item(videoId = "seen", channelUrl = CHANNEL),
            ),
            knownKeys = setOf("seen"),
            seeded = true,
            enabledChannels = listOf(ChannelNotificationsPreference(CHANNEL, enabled = true)),
        )

        assertEquals(listOf("new-enabled", "new-silent"), decision.keysToRecord)
        assertEquals(listOf("new-enabled"), decision.notified.map { it.video.id })
    }

    @Test
    fun `disabled channels are recorded but never notified`() {
        val decision = decideLocalNotifications(
            items = listOf(item(videoId = "a", channelUrl = CHANNEL)),
            knownKeys = emptySet(),
            seeded = true,
            enabledChannels = listOf(ChannelNotificationsPreference(CHANNEL, enabled = false)),
        )

        assertEquals(listOf("a"), decision.keysToRecord)
        assertTrue(decision.notified.isEmpty())
    }

    @Test
    fun `preference matches a channel url with an extra path segment on either side`() {
        val trailingOnItem = decideLocalNotifications(
            items = listOf(item(videoId = "a", channelUrl = "$CHANNEL/videos")),
            knownKeys = emptySet(),
            seeded = true,
            enabledChannels = listOf(ChannelNotificationsPreference(CHANNEL, enabled = true)),
        )
        val trailingOnPreference = decideLocalNotifications(
            items = listOf(item(videoId = "a", channelUrl = CHANNEL)),
            knownKeys = emptySet(),
            seeded = true,
            enabledChannels = listOf(ChannelNotificationsPreference("$CHANNEL/videos", enabled = true)),
        )

        assertEquals(listOf("a"), trailingOnItem.notified.map { it.video.id })
        assertEquals(listOf("a"), trailingOnPreference.notified.map { it.video.id })
    }

    @Test
    fun `a longer channel identifier is not treated as the same channel`() {
        val decision = decideLocalNotifications(
            items = listOf(item(videoId = "a", channelUrl = "${CHANNEL}extra")),
            knownKeys = emptySet(),
            seeded = true,
            enabledChannels = listOf(ChannelNotificationsPreference(CHANNEL, enabled = true)),
        )

        assertTrue(decision.notified.isEmpty())
    }

    @Test
    fun `newest first input is notified oldest first`() {
        val decision = decideLocalNotifications(
            items = listOf(
                item(videoId = "newest", channelUrl = CHANNEL),
                item(videoId = "older", channelUrl = CHANNEL),
            ),
            knownKeys = emptySet(),
            seeded = true,
            enabledChannels = listOf(ChannelNotificationsPreference(CHANNEL, enabled = true)),
        )

        assertEquals(listOf("older", "newest"), decision.notified.map { it.video.id })
    }

    private fun item(videoId: String, channelUrl: String) = NotificationItem(
        type = "subscription_new_video",
        title = "Video $videoId",
        createdAtMillis = 1L,
        publishedAtMillis = 1L,
        channelUrl = channelUrl,
        channelName = "Channel",
        channelAvatarUrl = "",
        video = Video(
            id = videoId,
            url = "https://example.test/watch?v=$videoId",
            title = "Video $videoId",
            thumbnailUrl = "",
            uploaderName = "Channel",
            uploaderUrl = channelUrl,
            uploaderAvatarUrl = "",
            uploaderVerified = false,
            durationSeconds = 0L,
            isLive = false,
            viewCount = 0L,
            uploadedAtMillis = 1L,
            isShortFormContent = false,
            shortDescription = null,
        ),
    )

    private companion object {
        const val CHANNEL = "https://www.youtube.com/channel/UCaaaaaaaaaaaaaaaaaaaaaa"
        const val OTHER_CHANNEL = "https://www.youtube.com/channel/UCbbbbbbbbbbbbbbbbbbbbbb"
    }
}
