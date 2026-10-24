package dev.typetype.android.feature.home

import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.library.HistoryItem
import dev.typetype.android.domain.server.Server

enum class HomeFeedKind { Recommended, Trending }

data class HomeState(
    val currentServer: Server? = null,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val feedKind: HomeFeedKind = HomeFeedKind.Recommended,
    val videos: List<Video> = emptyList(),
    val continueWatching: List<HistoryItem> = emptyList(),
    val hideHomeRecommendations: Boolean = false,
    val hideContinueWatching: Boolean = false,
    val nextCursor: String? = null,
    val errorMessage: String? = null,
    val errorRequestId: String? = null,
    val loadMoreError: Boolean = false,
)
