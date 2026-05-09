package dev.typetype.android.feature.player

sealed interface PlayerAction {
    data object OnToggleFavorite : PlayerAction
    data object OnToggleWatchLater : PlayerAction
    data object OnRetry : PlayerAction
    data object OnOpenPlaylistPicker : PlayerAction
    data object OnDismissPlaylistPicker : PlayerAction
    data class OnAddToPlaylist(val playlistId: String) : PlayerAction
    data class OnCreatePlaylistAndAdd(val name: String) : PlayerAction
    data class OnSaveProgress(val positionMillis: Long) : PlayerAction
}
