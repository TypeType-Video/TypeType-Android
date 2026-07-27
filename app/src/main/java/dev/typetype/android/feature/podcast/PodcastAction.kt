package dev.typetype.android.feature.podcast

sealed interface PodcastAction {
    data object OnRetry : PodcastAction
    data object OnLoadMore : PodcastAction
}
