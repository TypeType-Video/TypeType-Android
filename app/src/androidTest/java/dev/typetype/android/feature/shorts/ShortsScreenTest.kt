package dev.typetype.android.feature.shorts

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import dev.typetype.android.core.ui.components.VideoMenuAction
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

    @Test
    fun embeddedPlaybackCanAdvanceToTheNextShort() {
        val activeUrl = AtomicReference<String>()

        show(
            state = ShortsState(
                videos = listOf(video("one"), video("two")),
                isLoading = false,
            ),
            embeddedPlaybackEnabled = true,
            onActiveVideoChanged = { activeUrl.set(it?.url) },
            embeddedPlayback = { video, onAdvance ->
                Text("Embedded ${video.title}")
                Button(onClick = onAdvance) { Text("Advance") }
            },
        )

        composeRule.waitUntil { activeUrl.get() == "https://video/one" }
        composeRule.onNodeWithText("Embedded Short one").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "Open Short one in the full player",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Advance").performClick()
        composeRule.waitUntil { activeUrl.get() == "https://video/two" }
        composeRule.onNodeWithText("Embedded Short two").assertIsDisplayed()
    }

    @Test
    fun activeShortExposesNativeLibraryActions() {
        val selectedAction = AtomicReference<VideoMenuAction>()

        show(
            state = ShortsState(videos = listOf(video("one")), isLoading = false),
            embeddedPlaybackEnabled = true,
            onMenuAction = { action, _ -> selectedAction.set(action) },
        )

        composeRule.onNodeWithContentDescription("Add to favorites").performClick()
        assertEquals(VideoMenuAction.ToggleFavorite, selectedAction.get())
    }

    private fun show(
        state: ShortsState,
        onPlayVideo: (String) -> Unit = {},
        embeddedPlaybackEnabled: Boolean = false,
        onActiveVideoChanged: (Video?) -> Unit = {},
        embeddedPlayback: @Composable (Video, () -> Unit) -> Unit = { _, _ -> },
        onMenuAction: (VideoMenuAction, Video) -> Unit = { _, _ -> },
    ) {
        composeRule.setContent {
            TypeTypeTheme {
                ShortsScreen(
                    state = state,
                    onPlayVideo = onPlayVideo,
                    onOpenChannel = {},
                    onRefresh = {},
                    onLoadMore = {},
                    embeddedPlaybackEnabled = embeddedPlaybackEnabled,
                    onActiveVideoChanged = onActiveVideoChanged,
                    embeddedPlayback = embeddedPlayback,
                    onMenuAction = onMenuAction,
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
