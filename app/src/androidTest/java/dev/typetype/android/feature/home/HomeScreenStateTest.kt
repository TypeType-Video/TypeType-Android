package dev.typetype.android.feature.home

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.library.HistoryItem
import dev.typetype.android.feature.menu.VideoMenuScope
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cachedContentStaysVisibleDuringRefresh() {
        show(
            HomeState(
                isLoading = true,
                videos = listOf(video()),
                continueWatching = listOf(history()),
            ),
        )

        composeRule.onNodeWithText("CONTINUE WATCHING").assertIsDisplayed()
        composeRule.onNodeWithText("Continue title").assertIsDisplayed()
        composeRule.onNodeWithText("RECOMMENDED").assertIsDisplayed()
        composeRule.onNodeWithText("Recommended title").assertIsDisplayed()
    }

    @Test
    fun continueWatchingExposesProgressAndOpensTheStableVideoUrl() {
        val opened = AtomicReference<String>()
        show(
            state = HomeState(
                continueWatching = listOf(history()),
                hideHomeRecommendations = true,
            ),
            onPlayVideo = opened::set,
        )

        composeRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo(0.5f, 0f..1f),
            ),
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Continue title").performClick()

        assertEquals("https://video.example/continue", opened.get())
    }

    @Test
    fun emptyHomeHasAnExplicitState() {
        show(HomeState())

        composeRule.onNodeWithText("Nothing here yet").assertIsDisplayed()
    }

    private fun show(
        state: HomeState,
        onPlayVideo: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            TypeTypeTheme {
                HomeContent(
                    state = state,
                    menuScope = emptyMenuScope(),
                    onPlayVideo = onPlayVideo,
                    onOpenChannel = {},
                    onRetry = {},
                    onLoadMore = {},
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

    private fun history() = HistoryItem(
        id = "continue",
        url = "https://video.example/continue",
        title = "Continue title",
        thumbnailUrl = "",
        channelName = "Channel",
        durationSeconds = 120L,
        progressSeconds = 60L,
        watchedAtMillis = 1L,
    )

    private fun video() = Video(
        id = "recommended",
        url = "https://video.example/recommended",
        title = "Recommended title",
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
