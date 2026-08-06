package dev.typetype.android.feature.library.playlist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import org.junit.Rule
import org.junit.Test

class PlaylistScreenStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun initialLoadHasAnExplicitState() {
        show(isLoading = true)

        composeRule.onNodeWithText("Loading").assertIsDisplayed()
    }

    @Test
    fun completedLoadHasAnExplicitEmptyState() {
        show()

        composeRule.onNodeWithText("Empty playlist").assertIsDisplayed()
    }

    @Test
    fun failureKeepsTheRequestIdReviewable() {
        show(
            errorMessage = "Playlist is unavailable",
            errorRequestId = "request-playlist",
        )

        composeRule.onNodeWithText("Playlist is unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("Request request-playlist").assertIsDisplayed()
    }

    private fun show(
        isLoading: Boolean = false,
        errorMessage: String? = null,
        errorRequestId: String? = null,
    ) {
        composeRule.setContent {
            TypeTypeTheme {
                PlaylistScreen(
                    playlistId = "playlist-id",
                    title = "My playlist",
                    videos = emptyList(),
                    isLoading = isLoading,
                    isRefreshing = false,
                    isReordering = false,
                    isMutationInFlight = false,
                    errorMessage = errorMessage,
                    errorRequestId = errorRequestId,
                    onNavigateBack = {},
                    onPlayVideo = {},
                    onPlayQueue = { _, _, _ -> },
                    onOpenChannel = {},
                    onRetry = {},
                    onMoveVideo = { _, _ -> },
                    onRenamePlaylist = {},
                    onDeletePlaylist = {},
                )
            }
        }
    }
}
