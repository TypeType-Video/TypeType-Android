package dev.typetype.android.feature.publicplaylist

sealed interface PublicPlaylistAction {
    data object OnRetry : PublicPlaylistAction
    data object OnLoadMore : PublicPlaylistAction
    data object OnToggleSaved : PublicPlaylistAction
}
