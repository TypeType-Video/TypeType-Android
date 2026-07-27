package dev.typetype.android.feature.library

import dev.typetype.android.core.ui.components.LibrarySortMode
import dev.typetype.android.domain.library.Playlist
import dev.typetype.android.domain.library.PlaylistVideo
import dev.typetype.android.domain.publicplaylist.SavedPublicPlaylist

fun defaultSortFor(tab: LibraryTab): LibrarySortMode = when (tab) {
    LibraryTab.History -> LibrarySortMode.RecentFirst
    LibraryTab.Favorites -> LibrarySortMode.RecentFirst
    LibraryTab.WatchLater -> LibrarySortMode.RecentFirst
    LibraryTab.Playlists -> LibrarySortMode.RecentFirst
    LibraryTab.SavedPlaylists -> LibrarySortMode.RecentFirst
}

fun sortOptionsFor(tab: LibraryTab): List<LibrarySortMode> = when (tab) {
    LibraryTab.History,
    LibraryTab.Favorites,
    LibraryTab.WatchLater -> listOf(
        LibrarySortMode.RecentFirst,
        LibrarySortMode.OldestFirst,
        LibrarySortMode.TitleAZ,
        LibrarySortMode.TitleZA,
    )
    LibraryTab.Playlists,
    LibraryTab.SavedPlaylists -> listOf(
        LibrarySortMode.RecentFirst,
        LibrarySortMode.OldestFirst,
        LibrarySortMode.NameAZ,
        LibrarySortMode.NameZA,
    )
}

fun sortPlaylistVideos(
    items: List<PlaylistVideo>,
    mode: LibrarySortMode,
): List<PlaylistVideo> = when (mode) {
    LibrarySortMode.OldestFirst -> items.sortedBy { it.position }
    LibrarySortMode.TitleAZ -> items.sortedBy { it.title.lowercase() }
    LibrarySortMode.TitleZA -> items.sortedByDescending { it.title.lowercase() }
    else -> items.sortedByDescending { it.position }
}

fun sortPlaylists(items: List<Playlist>, mode: LibrarySortMode): List<Playlist> = when (mode) {
    LibrarySortMode.OldestFirst -> items.sortedBy { it.createdAtMillis }
    LibrarySortMode.NameAZ -> items.sortedBy { it.name.lowercase() }
    LibrarySortMode.NameZA -> items.sortedByDescending { it.name.lowercase() }
    else -> items.sortedByDescending { it.createdAtMillis }
}

private fun matchesFilter(text: String, filter: String): Boolean =
    filter.isBlank() || text.contains(filter.trim(), ignoreCase = true)

fun filterPlaylistVideos(items: List<PlaylistVideo>, filter: String): List<PlaylistVideo> =
    if (filter.isBlank()) items
    else items.filter { matchesFilter(it.title, filter) }

fun filterPlaylists(items: List<Playlist>, filter: String): List<Playlist> =
    if (filter.isBlank()) items
    else items.filter { matchesFilter(it.name, filter) }

fun sortSavedPlaylists(
    items: List<SavedPublicPlaylist>,
    mode: LibrarySortMode,
): List<SavedPublicPlaylist> = when (mode) {
    LibrarySortMode.OldestFirst -> items.sortedBy { it.savedAtMillis }
    LibrarySortMode.NameAZ -> items.sortedBy { it.title.lowercase() }
    LibrarySortMode.NameZA -> items.sortedByDescending { it.title.lowercase() }
    else -> items.sortedByDescending { it.savedAtMillis }
}

fun filterSavedPlaylists(
    items: List<SavedPublicPlaylist>,
    filter: String,
): List<SavedPublicPlaylist> = if (filter.isBlank()) {
    items
} else {
    items.filter {
        matchesFilter(it.title, filter) || matchesFilter(it.uploaderName, filter)
    }
}
