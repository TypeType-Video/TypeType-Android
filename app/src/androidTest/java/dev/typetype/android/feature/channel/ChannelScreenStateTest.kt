package dev.typetype.android.feature.channel

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import dev.typetype.android.core.ui.components.LocalAnimatedStatePlayback
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.channel.Channel
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.feature.menu.VideoMenuScope
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChannelScreenStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun initialLoadHasAnAccessibleState() {
        showScreen(ChannelState(isLoading = true))

        composeRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
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

    @Test
    fun discoveryUsesCompactSectionAndSortMenus() {
        val actions = mutableListOf<ChannelAction>()
        showContent(
            ChannelState(
                isLoading = false,
                channel = channel(videos = listOf(video())),
                supportsYouTubeDiscovery = true,
            ),
            onAction = actions::add,
        )

        composeRule.onNodeWithContentDescription("Channel section").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Video order").assertIsDisplayed()
        composeRule.onNodeWithText("Search this channel").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Channel section").performClick()
        composeRule.onNode(hasText("Videos") and isSelected()).assertIsSelected()
        composeRule.onNodeWithText("Live").performClick()

        assertTrue(actions.contains(ChannelAction.OnSelectTab(ChannelTab.Live)))
    }

    @Test
    fun topBarSearchSubmitsWithoutPermanentSearchChrome() {
        val actions = mutableListOf<ChannelAction>()
        var state by mutableStateOf(
            ChannelState(
                isLoading = false,
                channel = channel(videos = listOf(video())),
                supportsYouTubeDiscovery = true,
            ),
        )
        var searchExpanded by mutableStateOf(false)
        composeRule.setContent {
            TypeTypeTheme {
                ChannelTopBar(
                    state = state,
                    searchExpanded = searchExpanded,
                    onSearchExpandedChange = { searchExpanded = it },
                    onNavigateBack = {},
                    onAction = { action ->
                        actions += action
                        if (action is ChannelAction.OnSearchInputChanged) {
                            state = state.copy(searchInput = action.value)
                        } else if (action == ChannelAction.OnClearSearchInput) {
                            state = state.copy(searchInput = "")
                        }
                    },
                )
            }
        }

        composeRule.onNodeWithText("Search this channel").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Search this channel").performClick()
        composeRule.onNodeWithText("Search this channel").performTextInput("compose")
        composeRule.onNodeWithContentDescription("Clear channel search").performClick()
        composeRule.onNodeWithText("Search this channel").performTextInput("compose")
        composeRule.onNodeWithText("compose").performImeAction()

        assertTrue(actions.contains(ChannelAction.OnSearchInputChanged("compose")))
        assertTrue(actions.contains(ChannelAction.OnClearSearchInput))
        assertTrue(actions.contains(ChannelAction.OnSubmitSearch))
        composeRule.onNodeWithText("Search this channel").assertDoesNotExist()
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

    private fun showContent(
        state: ChannelState,
        onAction: (ChannelAction) -> Unit = {},
    ) {
        composeRule.setContent {
            TypeTypeTheme {
                ChannelContentGrid(
                    state = state,
                    onPlayVideo = {},
                    onOpenPodcast = {},
                    onOpenPlaylist = {},
                    onAction = onAction,
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
