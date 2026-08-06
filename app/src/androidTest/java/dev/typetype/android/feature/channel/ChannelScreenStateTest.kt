package dev.typetype.android.feature.channel

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import dev.typetype.android.core.ui.components.LocalAnimatedStatePlayback
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.channel.Channel
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.feature.menu.VideoMenuScope
import org.junit.Rule
import org.junit.Test

class ChannelScreenStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun initialLoadHasAnAccessibleState() {
        showScreen(ChannelState(isLoading = true))

        composeRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
    }

    @Test
    fun fatalErrorKeepsTheRequestIdReviewable() {
        showScreen(
            ChannelState(
                isLoading = false,
                errorMessage = "Channel is unavailable",
                errorRequestId = "request-channel",
            ),
        )

        composeRule.onNodeWithText("Channel is unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("Request request-channel").assertIsDisplayed()
    }

    @Test
    fun emptyChannelHasAnExplicitState() {
        showContent(ChannelState(isLoading = false, channel = channel()))

        composeRule.onNodeWithText("No videos available").assertIsDisplayed()
    }

    @Test
    fun cachedContentStaysVisibleDuringRefresh() {
        showContent(
            ChannelState(
                isLoading = true,
                channel = channel(videos = listOf(video())),
            ),
        )

        composeRule.onNodeWithText("Channel video").assertIsDisplayed()
    }

    private fun showScreen(state: ChannelState) {
        composeRule.setContent {
            CompositionLocalProvider(LocalAnimatedStatePlayback provides false) {
                TypeTypeTheme {
                    ChannelScreen(
                        state = state,
                        onNavigateBack = {},
                        onPlayVideo = {},
                        onOpenPodcast = {},
                        onOpenPlaylist = {},
                        onAction = {},
                    )
                }
            }
        }
    }

    private fun showContent(state: ChannelState) {
        composeRule.setContent {
            TypeTypeTheme {
                ChannelContentGrid(
                    state = state,
                    onNavigateBack = {},
                    onPlayVideo = {},
                    onOpenPodcast = {},
                    onOpenPlaylist = {},
                    onAction = {},
                    menuScope = emptyMenuScope(),
                )
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

    private fun channel(videos: List<Video> = emptyList()) = Channel(
        name = "Channel",
        description = "Description",
        avatarUrl = "",
        bannerUrl = null,
        subscriberCount = 1L,
        verified = false,
        videos = videos,
    )

    private fun video() = Video(
        id = "channel-video",
        url = "https://video.example/channel-video",
        title = "Channel video",
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
