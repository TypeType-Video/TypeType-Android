package dev.typetype.android.feature.shorts

import dev.typetype.android.domain.feed.Video

data class ShortsState(
    val videos: List<Video> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val hidden: Boolean = false,
    val errorMessage: String? = null,
    val errorRequestId: String? = null,
    val loadMoreError: Boolean = false,
)

sealed interface ShortsAction {
    data object Refresh : ShortsAction
    data object LoadMore : ShortsAction
}
