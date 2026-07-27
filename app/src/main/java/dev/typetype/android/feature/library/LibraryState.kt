package dev.typetype.android.feature.library

import dev.typetype.android.domain.library.Playlist
import dev.typetype.android.domain.library.PlaylistVideo
import dev.typetype.android.domain.publicplaylist.SavedPublicPlaylist

enum class LibraryTab { History, Favorites, WatchLater, Playlists, SavedPlaylists }

data class LibraryState(
    val selectedTab: LibraryTab = LibraryTab.History,
    val isLoading: Boolean = false,
    val historyItemCount: Int = 0,
    val isLoadingMoreHistory: Boolean = false,
    val historyHasMore: Boolean = true,
    val favorites: List<PlaylistVideo> = emptyList(),
    val watchLater: List<PlaylistVideo> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val savedPlaylists: List<SavedPublicPlaylist> = emptyList(),
    val canSavePublicPlaylists: Boolean = false,
    val isPlaylistMutationInFlight: Boolean = false,
    val isSavedPlaylistMutationInFlight: Boolean = false,
    val errorMessage: String? = null,
    val errorRequestId: String? = null,
    val lastSuccessfulSyncAtMillis: Long? = null,
    val syncRequestId: String? = null,
    val pendingWriteCount: Int = 0,
    val failedWriteCount: Int = 0,
)

internal fun LibraryState.shouldShowInitialLoader(): Boolean =
    isLoading && !hasVisibleContent() && lastSuccessfulSyncAtMillis == null

private fun LibraryState.hasVisibleContent(): Boolean = when (selectedTab) {
    LibraryTab.History -> historyItemCount > 0
    LibraryTab.Favorites -> favorites.isNotEmpty()
    LibraryTab.WatchLater -> watchLater.isNotEmpty()
    LibraryTab.Playlists -> playlists.isNotEmpty()
    LibraryTab.SavedPlaylists -> savedPlaylists.isNotEmpty()
}
