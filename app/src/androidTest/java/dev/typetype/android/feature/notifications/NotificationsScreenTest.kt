package dev.typetype.android.feature.notifications

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.notifications.NotificationItem
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NotificationsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyStateExplainsThatThereAreNoNotifications() {
        setScreen(NotificationsState(isLoading = false))

        composeRule.onNodeWithText("No notifications yet").assertIsDisplayed()
    }

    @Test
    fun unreadNotificationsCanBeMarkedRead() {
        val marked = AtomicBoolean(false)
        setScreen(
            state = NotificationsState(isLoading = false, unreadCount = 3),
            onAction = { if (it == NotificationsAction.MarkAllRead) marked.set(true) },
        )

        composeRule.onNodeWithContentDescription("Mark all read").performClick()

        assertTrue(marked.get())
    }

    @Test
    fun selectingANotificationOpensItsVideo() {
        val opened = AtomicReference<String>()
        setScreen(
            state = NotificationsState(
                items = listOf(notification()),
                isLoading = false,
            ),
            onPlayVideo = opened::set,
        )

        composeRule.onNodeWithText("New video").assertIsDisplayed()
        composeRule.onNodeWithText("Video title").performClick()

        assertEquals("video-url", opened.get())
    }

    private fun setScreen(
        state: NotificationsState,
        onPlayVideo: (String) -> Unit = {},
        onAction: (NotificationsAction) -> Unit = {},
    ) {
        composeRule.setContent {
            TypeTypeTheme {
                NotificationsScreen(
                    state = state,
                    onNavigateBack = {},
                    onPlayVideo = onPlayVideo,
                    onAction = onAction,
                )
            }
        }
    }

    private fun notification() = NotificationItem(
        type = "subscription_new_video",
        title = "Channel uploaded a new video",
        createdAtMillis = 100L,
        publishedAtMillis = 100L,
        channelUrl = "channel-url",
        channelName = "Channel",
        channelAvatarUrl = "",
        video = Video(
            id = "video",
            url = "video-url",
            title = "Video title",
            thumbnailUrl = "",
            uploaderName = "Channel",
            uploaderUrl = "channel-url",
            uploaderAvatarUrl = "",
            uploaderVerified = false,
            durationSeconds = 60L,
            isLive = false,
            viewCount = 1L,
            uploadedAtMillis = 100L,
            isShortFormContent = false,
            shortDescription = null,
        ),
    )
}
