package dev.typetype.android.feature.shorts

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.feed.Video
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ShortsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pagerShowsAPlayableShort() {
        val playedUrl = AtomicReference<String>()
        val video = video("one")

        show(
            state = ShortsState(videos = listOf(video), isLoading = false),
            onPlayVideo = playedUrl::set,
        )

        composeRule.onNodeWithTag(SHORTS_PAGER_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Short one").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Play Short one").performClick()
        assertEquals(video.url, playedUrl.get())
    }

    @Test
    fun emptyFeedHasAnExplicitState() {
        show(ShortsState(isLoading = false))

        composeRule.onNodeWithText("No Shorts are available yet.").assertIsDisplayed()
    }

    @Test
    fun failureKeepsTheRequestIdReviewable() {
        show(
            ShortsState(
                isLoading = false,
                errorMessage = "The instance could not load Shorts",
                errorRequestId = "request-shorts",
            ),
        )

        composeRule.onNodeWithText("The instance could not load Shorts").assertIsDisplayed()
        composeRule.onNodeWithText("Request request-shorts").assertIsDisplayed()
    }

    private fun show(
        state: ShortsState,
        onPlayVideo: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            TypeTypeTheme {
                ShortsScreen(
                    state = state,
                    onPlayVideo = onPlayVideo,
                    onOpenChannel = {},
                    onRefresh = {},
                    onLoadMore = {},
                )
            }
        }
    }

    private fun video(id: String) = Video(
        id = id,
        url = "https://video/$id",
        title = "Short $id",
        thumbnailUrl = "",
        uploaderName = "Channel",
        uploaderUrl = "https://channel",
        uploaderAvatarUrl = "",
        uploaderVerified = false,
        durationSeconds = 30,
        isLive = false,
        viewCount = 1,
        uploadedAtMillis = 1,
        isShortFormContent = true,
        shortDescription = null,
    )
}
