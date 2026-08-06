package dev.typetype.android.feature.library

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.paging.PagingData
import dev.typetype.android.core.ui.components.LocalAnimatedStatePlayback
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.library.HistoryItem
import dev.typetype.android.domain.library.Playlist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class LibraryScreenStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun initialLoadHasAnAccessibleState() {
        show(LibraryState(isLoading = true))

        composeRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
    }

    @Test
    fun playlistFailureKeepsTheRequestIdReviewable() {
        show(
            LibraryState(
                selectedTab = LibraryTab.Playlists,
                errorMessage = "Library is unavailable",
                syncRequestId = "request-library",
            ),
        )

        composeRule.onNodeWithText("Library is unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("Request request-library").assertIsDisplayed()
    }

    @Test
    fun completedPlaylistLoadHasAnExplicitEmptyState() {
        show(LibraryState(selectedTab = LibraryTab.Playlists))

        composeRule.onNodeWithText("No playlists yet").assertIsDisplayed()
    }

    @Test
    fun cachedPlaylistsStayVisibleDuringRefresh() {
        show(
            LibraryState(
                selectedTab = LibraryTab.Playlists,
                isLoading = true,
                playlists = listOf(playlist()),
            ),
        )

        composeRule.onNodeWithText("Offline rides").assertIsDisplayed()
    }

    private fun show(state: LibraryState) {
        composeRule.setContent {
            CompositionLocalProvider(LocalAnimatedStatePlayback provides false) {
                TypeTypeTheme {
                    LibraryScreen(
                        state = state,
                        historyPagingData = emptyHistory(),
                        onPlayVideo = {},
                        onOpenPlaylist = {},
                        onOpenPublicPlaylist = {},
                        onOpenChannel = {},
                        onAction = {},
                        onHistoryQueryChange = { _, _, _ -> },
                    )
                }
            }
        }
    }

    private fun emptyHistory(): Flow<PagingData<HistoryItem>> = flowOf(PagingData.empty())

    private fun playlist() = Playlist(
        id = "offline-rides",
        name = "Offline rides",
        description = "",
        videos = emptyList(),
        createdAtMillis = 1L,
    )
}
