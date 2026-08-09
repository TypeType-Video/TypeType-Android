package dev.typetype.android.feature.subscriptions

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import dev.typetype.android.core.ui.components.LocalAnimatedStatePlayback
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.feature.menu.VideoMenuScope
import org.junit.Rule
import org.junit.Test

class SubscriptionsScreenStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun initialLoadHasAnAccessibleState() {
        show(SubscriptionsState(isLoading = true))

        composeRule.onNodeWithContentDescription("Preparing subscriptions").assertIsDisplayed()
    }

    @Test
    fun fatalErrorKeepsTheRequestIdReviewable() {
        show(
            SubscriptionsState(
                errorMessage = "Subscriptions are unavailable",
                errorRequestId = "request-subscriptions",
            ),
        )

        composeRule.onNodeWithText("Subscriptions are unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("Request request-subscriptions").assertIsDisplayed()
    }

    @Test
    fun emptyFeedHasAnExplicitState() {
        show(SubscriptionsState())

        composeRule.onNodeWithText("Your subscriptions will appear here").assertIsDisplayed()
    }

    @Test
    fun cachedContentStaysVisibleDuringRefresh() {
        show(
            SubscriptionsState(
                isLoading = true,
                videos = listOf(video()),
            ),
        )

        composeRule.onNodeWithText("Subscription video").assertIsDisplayed()
    }

    @Test
    fun cachedContentStaysVisibleWhenRefreshFails() {
        show(
            SubscriptionsState(
                videos = listOf(video()),
                errorMessage = "Subscriptions are temporarily unavailable",
                errorRequestId = "request-refresh",
            ),
        )

        composeRule.onNodeWithText("Subscription video").assertIsDisplayed()
    }

    private fun show(state: SubscriptionsState) {
        composeRule.setContent {
            CompositionLocalProvider(LocalAnimatedStatePlayback provides false) {
                TypeTypeTheme {
                    SubscriptionsContent(
                        state = state,
                        visibleVideos = state.videos,
                        onPlayVideo = {},
                        onOpenChannel = {},
                        onRetry = {},
                        onLoadMore = {},
                        menuScope = emptyMenuScope(),
                    )
                }
            }
        }
    }

    private fun emptyMenuScope() = VideoMenuScope(
        watchedUrls = emptySet(),
        blockedVideoUrls = emptySet(),
        blockedChannelUrls = emptySet(),
        blockedKeywords = emptySet(),
        favorites = emptySet(),
        watchLater = emptySet(),
        onAction = { _, _ -> },
    )

    private fun video() = Video(
        id = "subscription",
        url = "https://video.example/subscription",
        title = "Subscription video",
        thumbnailUrl = "",
        uploaderName = "Channel",
        uploaderUrl = "https://channel.example/channel",
        uploaderAvatarUrl = "",
        uploaderVerified = false,
        durationSeconds = 180L,
        isLive = false,
        viewCount = 1L,
        uploadedAtMillis = 1L,
        isShortFormContent = false,
        shortDescription = null,
    )
}
