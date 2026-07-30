package dev.typetype.android.feature.player

import dev.typetype.android.domain.download.DownloadSelection

sealed interface PlayerAction {
    data object OnToggleFavorite : PlayerAction
    data object OnToggleWatchLater : PlayerAction
    data object OnRetry : PlayerAction
    data object OnAdvanceQueue : PlayerAction
    data object OnCancelQueueAutoplay : PlayerAction
    data object OnToggleQueueAutoplayPause : PlayerAction
    data class OnDownload(val selection: DownloadSelection) : PlayerAction
    data object OnOpenPlaylistPicker : PlayerAction
    data object OnDismissPlaylistPicker : PlayerAction
    data class OnAddToPlaylist(val playlistId: String) : PlayerAction
    data class OnCreatePlaylistAndAdd(val name: String) : PlayerAction
    data class OnSaveProgress(val positionMillis: Long) : PlayerAction
}
