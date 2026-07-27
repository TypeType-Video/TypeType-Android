package dev.typetype.android.feature.publicplaylist

import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.search.SearchPlaylist

data class PublicPlaylistState(
    val playlist: SearchPlaylist? = null,
    val videos: List<Video> = emptyList(),
    val nextPage: String? = null,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val errorRequestId: String? = null,
    val loadMoreError: Boolean = false,
    val canSave: Boolean = false,
    val savedItemId: String? = null,
    val saveInFlight: Boolean = false,
    val saveErrorMessage: String? = null,
)
