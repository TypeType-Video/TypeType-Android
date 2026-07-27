package dev.typetype.android.feature.library

sealed interface LibraryAction {
    data class OnTabSelect(val tab: LibraryTab) : LibraryAction
    data class OnCreatePlaylist(val name: String) : LibraryAction
    data class OnRenamePlaylist(val playlistId: String, val name: String) : LibraryAction
    data class OnDeletePlaylist(val playlistId: String) : LibraryAction
    data class OnRemoveSavedPlaylist(val savedPlaylistId: String) : LibraryAction
    data object OnRefresh : LibraryAction
    data object OnRetry : LibraryAction
    data object OnLoadMoreHistory : LibraryAction
}
