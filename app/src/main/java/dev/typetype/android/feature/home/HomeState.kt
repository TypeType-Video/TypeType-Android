package dev.typetype.android.feature.home

import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.server.Server

enum class TopSectionKind { Subscriptions, Trending }

data class HomeState(
    val currentServer: Server? = null,
    val isLoading: Boolean = false,
    val topSectionKind: TopSectionKind = TopSectionKind.Trending,
    val topSectionVideos: List<Video> = emptyList(),
    val topSectionError: String? = null,
    val recommendations: List<Video> = emptyList(),
    val recommendationsError: String? = null,
)
