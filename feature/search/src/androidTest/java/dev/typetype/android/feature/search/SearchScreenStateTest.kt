package dev.typetype.android.feature.search

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import dev.typetype.android.core.ui.components.LocalAnimatedStatePlayback
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.search.SearchFilterOption
import dev.typetype.android.feature.menu.VideoMenuScope
import org.junit.Rule
import org.junit.Test

class SearchScreenStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingSearchHasAnAccessibleState() {
        show(SearchState(query = "kotlin", isLoading = true))

        composeRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
    }

    @Test
    fun searchFieldExposesItsPurposeWhenEmpty() {
        show(SearchState())

        composeRule.onNodeWithContentDescription("Search videos…").assertIsDisplayed()
    }

    @Test
    fun fatalErrorKeepsTheRequestIdReviewable() {
        show(
            SearchState(
                query = "kotlin",
                errorMessage = "Search is unavailable",
                errorRequestId = "request-search",
            ),
        )

        composeRule.onNodeWithText("Search is unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("Request request-search").assertIsDisplayed()
    }

    @Test
    fun recentSearchesRemainAvailableBeforeSubmitting() {
        show(SearchState(searchHistory = listOf("Compose accessibility")))

        composeRule.onNodeWithText("Recent searches").assertIsDisplayed()
        composeRule.onNodeWithText("Compose accessibility").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Clear all search history").assertIsDisplayed()
    }

    @Test
    fun duplicateRecentSearchesHaveUniqueLazyListKeys() {
        show(SearchState(searchHistory = listOf("Compose", "Compose")))

        composeRule.onAllNodesWithText("Compose").assertCountEquals(2)
    }

    @Test
    fun filtersStayHiddenUntilSearchFinishesSubmitting() {
        show(searchState())

        composeRule.onNodeWithContentDescription("Content type").assertDoesNotExist()
    }

    @Test
    fun filtersAppearOnlyWithResultsState() {
        composeRule.setContent {
            TypeTypeTheme {
                SearchFilterBar(
                    contentFilters = searchState().contentFilters,
                    filterGroups = emptyList(),
                    selectedContent = null,
                    selectedFilters = emptyList(),
                    onContentSelect = {},
                    onFilterToggle = { _, _ -> },
                    onResetFilters = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Content type").assertIsDisplayed()
    }

    @Test
    fun completedSearchHasAnExplicitEmptyState() {
        composeRule.setContent {
            TypeTypeTheme {
                SearchResultsContent(
                    state = SearchState(query = "missing", hasSearched = true),
                    onPlayVideo = {},
                    onOpenChannel = {},
                    onOpenPlaylist = {},
                    onSearchSuggestion = {},
                    onLoadMore = {},
                    menuScope = emptyMenuScope(),
                )
            }
        }

        composeRule.onNodeWithText("No results for \"missing\"").assertIsDisplayed()
    }

    private fun show(state: SearchState) {
        composeRule.setContent {
            CompositionLocalProvider(LocalAnimatedStatePlayback provides false) {
                TypeTypeTheme {
                    SearchScreen(
                        state = state,
                        onNavigateBack = {},
                        onPlayVideo = {},
                        onOpenChannel = {},
                        onOpenPlaylist = {},
                        onAction = {},
                    )
                }
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

    private fun searchState() = SearchState(
        query = "kotlin",
        contentFilters = listOf(
            SearchFilterOption(value = "video", label = "video", isDefault = false),
        ),
    )
}
