package dev.typetype.android.feature.podcast

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import dev.typetype.android.core.ui.components.LocalAnimatedStatePlayback
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.podcast.Podcast
import dev.typetype.android.feature.menu.VideoMenuScope
import org.junit.Rule
import org.junit.Test

class PodcastScreenStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun initialLoadHasAnAccessibleState() {
        showScreen(PodcastState())
        composeRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
    }

    @Test
    fun fatalErrorKeepsTheRequestIdReviewable() {
        showScreen(
            PodcastState(
                isLoading = false,
                errorMessage = "Podcast is unavailable",
                errorRequestId = "request-podcast",
            ),
        )
        composeRule.onNodeWithText("Podcast is unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("Request request-podcast").assertIsDisplayed()
    }

    @Test
    fun emptyPodcastHasAnExplicitState() {
        composeRule.setContent {
            TypeTypeTheme {
                PodcastContentGrid(
                    state = PodcastState(isLoading = false, podcast = podcast()),
                    onPlayVideo = {},
                    onPlayQueue = { _, _, _ -> },
                    onOpenChannel = {},
                    onLoadMore = {},
                    menuScope = emptyMenuScope(),
                )
            }
        }
        composeRule.onNodeWithText("This podcast has no available episodes.").assertIsDisplayed()
    }

    private fun showScreen(state: PodcastState) {
        composeRule.setContent {
            CompositionLocalProvider(LocalAnimatedStatePlayback provides false) {
                TypeTypeTheme {
                    PodcastScreen(
                        state = state,
                        onNavigateBack = {},
                        onPlayVideo = {},
                        onPlayQueue = { _, _, _ -> },
                        onOpenChannel = {},
                        onAction = {},
                    )
                }
            }
        }
    }

    private fun podcast() = Podcast(
        id = "podcast",
        title = "Podcast",
        url = "https://podcast.example/show",
        thumbnailUrl = "",
        uploaderName = "Channel",
        episodeCount = 0L,
        type = "playlist",
    )

    private fun emptyMenuScope() = VideoMenuScope(
        watchedUrls = emptySet(),
        blockedVideoUrls = emptySet(),
        blockedChannelUrls = emptySet(),
        blockedKeywords = emptySet(),
        favorites = emptySet(),
        watchLater = emptySet(),
        onAction = { _, _ -> },
    )
}
