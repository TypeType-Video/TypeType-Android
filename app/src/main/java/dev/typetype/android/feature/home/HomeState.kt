package dev.typetype.android.feature.home

import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.server.Server

data class HomeState(
    val currentServer: Server? = null,
    val isLoading: Boolean = false,
    val videos: List<Video> = emptyList(),
    val errorMessage: String? = null,
)
