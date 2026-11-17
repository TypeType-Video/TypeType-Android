package dev.typetype.android.feature.live

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.typetype.android.core.ui.components.LocalAnimatedStatePlayback
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.feed.Video
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LiveContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun livesAreListedUnderTheirHeaderAndOpenTheirChannelLive() {
        val opened = AtomicReference<String>()
        show(LiveState(videos = listOf(video())), onPlayVideo = opened::set)

        composeRule.onNodeWithText("LIVE NOW ON TWITCH").assertIsDisplayed()
        composeRule.onNodeWithText("Building a castle").performClick()

        assertEquals("https://www.twitch.tv/space", opened.get())
    }

    @Test
    fun aFailedFirstPageOffersARetry() {
        var retried = false
        show(LiveState(errorMessage = "Twitch lives could not be loaded"), onRetry = { retried = true })

        composeRule.onNodeWithText("Twitch lives could not be loaded").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()

        assertEquals(true, retried)
    }

    private fun show(state: LiveState, onPlayVideo: (String) -> Unit = {}, onRetry: () -> Unit = {}) {
        composeRule.setContent {
            CompositionLocalProvider(LocalAnimatedStatePlayback provides false) {
                TypeTypeTheme {
                    LiveContent(state = state, onPlayVideo = onPlayVideo, onRetry = onRetry, onLoadMore = {})
                }
            }
        }
    }

    private fun video() = Video(
        id = "https://www.twitch.tv/space",
        url = "https://www.twitch.tv/space",
        title = "Building a castle",
        thumbnailUrl = "",
        uploaderName = "Space",
        uploaderUrl = "https://www.twitch.tv/space",
        uploaderAvatarUrl = "",
        uploaderVerified = false,
        durationSeconds = 0L,
        isLive = true,
        viewCount = 1_084L,
        uploadedAtMillis = 1L,
        isShortFormContent = false,
        shortDescription = null,
        isLiveContent = true,
    )
}
