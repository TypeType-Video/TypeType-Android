package dev.typetype.android.feature.publicplaylist

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import dev.typetype.android.core.ui.components.LocalAnimatedStatePlayback
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.search.SearchPlaylist
import dev.typetype.android.feature.menu.VideoMenuScope
import org.junit.Rule
import org.junit.Test

class PublicPlaylistScreenStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun initialLoadHasAnAccessibleState() {
        showScreen(PublicPlaylistState())
        composeRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
    }

    @Test
    fun fatalErrorKeepsTheRequestIdReviewable() {
        showScreen(
            PublicPlaylistState(
                isLoading = false,
                errorMessage = "Playlist is unavailable",
                errorRequestId = "request-playlist",
            ),
        )
        composeRule.onNodeWithText("Playlist is unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("Request request-playlist").assertIsDisplayed()
    }

    @Test
    fun emptyPlaylistHasAnExplicitState() {
        composeRule.setContent {
            TypeTypeTheme {
                PublicPlaylistContentGrid(
                    state = PublicPlaylistState(isLoading = false, playlist = playlist()),
                    onPlayVideo = {},
                    onPlayQueue = { _, _, _ -> },
                    onOpenChannel = {},
                    onLoadMore = {},
                    onToggleSaved = {},
                    menuScope = emptyMenuScope(),
                )
            }
        }
        composeRule.onNodeWithText("This playlist has no available videos.").assertIsDisplayed()
    }

    private fun showScreen(state: PublicPlaylistState) {
        composeRule.setContent {
            CompositionLocalProvider(LocalAnimatedStatePlayback provides false) {
                TypeTypeTheme {
                    PublicPlaylistScreen(
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

    private fun playlist() = SearchPlaylist(
        id = "playlist",
        title = "Playlist",
        url = "https://playlist.example/list",
        thumbnailUrl = "",
        uploaderName = "Channel",
        streamCount = 0L,
        playlistType = "playlist",
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
