package dev.typetype.android.feature.podcast

import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.podcast.Podcast

data class PodcastState(
    val podcast: Podcast? = null,
    val episodes: List<Video> = emptyList(),
    val nextPage: String? = null,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val errorRequestId: String? = null,
    val loadMoreError: Boolean = false,
)
