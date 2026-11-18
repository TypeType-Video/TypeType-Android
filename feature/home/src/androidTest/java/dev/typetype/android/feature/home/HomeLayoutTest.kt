package dev.typetype.android.feature.home

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.typetype.android.core.ui.components.LocalAnimatedStatePlayback
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.feed.HomeRecommendationRow
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.preferences.HomeLayout
import dev.typetype.android.feature.menu.VideoMenuScope
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rowsShowTheirCategoryTitles() {
        show(HomeLayout.Rows)

        composeRule.onNodeWithText("By category").assertIsSelected()
        composeRule.onNodeWithText("Space row").assertIsDisplayed()
        composeRule.onNodeWithText("First video").assertIsDisplayed()
    }

    @Test
    fun gridShowsTheVideosWithoutCategoryTitles() {
        show(HomeLayout.Grid)

        composeRule.onNodeWithText("As usual").assertIsSelected()
        composeRule.onNodeWithText("Space row").assertDoesNotExist()
        composeRule.onNodeWithText("First video").assertIsDisplayed()
    }

    @Test
    fun choosingALayoutSendsIt() {
        val chosen = AtomicReference<HomeAction>()
        show(HomeLayout.Rows, onAction = chosen::set)

        composeRule.onNodeWithText("As usual").performClick()

        assertEquals(HomeAction.SetLayout(HomeLayout.Grid), chosen.get())
    }

    private fun show(layout: HomeLayout, onAction: (HomeAction) -> Unit = {}) {
        val state = HomeState(
            videos = listOf(video("first", "First video")),
            layout = layout,
            recommender = HomeRecommenderState(
                algorithm = "typetype",
                rows = listOf(
                    HomeRecommendationRow(
                        id = "space",
                        title = "Space row",
                        reason = "topic",
                        reasonRef = null,
                        videoUrls = listOf("https://video.example/first"),
                    ),
                ),
            ),
        )
        composeRule.setContent {
            CompositionLocalProvider(LocalAnimatedStatePlayback provides false) {
                TypeTypeTheme {
                    HomeContent(
                        state = state,
                        menuScope = VideoMenuScope(
                            watchedUrls = emptySet(),
                            blockedVideoUrls = emptySet(),
                            blockedChannelUrls = emptySet(),
                            blockedKeywords = emptySet(),
                            favorites = emptySet(),
                            watchLater = emptySet(),
                            onAction = { _, _ -> },
                        ),
                        onPlayVideo = {},
                        onOpenChannel = {},
                        onRetry = {},
                        onLoadMore = {},
                        onAction = onAction,
                    )
                }
            }
        }
    }

    private fun video(id: String, title: String) = Video(
        id = id,
        url = "https://video.example/$id",
        title = title,
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
