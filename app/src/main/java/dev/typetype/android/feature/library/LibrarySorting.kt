package dev.typetype.android.feature.library

import dev.typetype.android.core.ui.components.LibrarySortMode
import dev.typetype.android.domain.library.HistoryItem
import dev.typetype.android.domain.library.Playlist
import dev.typetype.android.domain.library.PlaylistVideo

fun defaultSortFor(tab: LibraryTab): LibrarySortMode = when (tab) {
    LibraryTab.History -> LibrarySortMode.RecentFirst
    LibraryTab.Favorites -> LibrarySortMode.RecentFirst
    LibraryTab.WatchLater -> LibrarySortMode.RecentFirst
    LibraryTab.Playlists -> LibrarySortMode.RecentFirst
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
    LibraryTab.Playlists -> listOf(
        LibrarySortMode.RecentFirst,
        LibrarySortMode.OldestFirst,
        LibrarySortMode.NameAZ,
        LibrarySortMode.NameZA,
    )
}

fun sortHistory(items: List<HistoryItem>, mode: LibrarySortMode): List<HistoryItem> = when (mode) {
    LibrarySortMode.OldestFirst -> items.sortedBy { it.watchedAtMillis }
    LibrarySortMode.TitleAZ -> items.sortedBy { it.title.lowercase() }
    LibrarySortMode.TitleZA -> items.sortedByDescending { it.title.lowercase() }
    else -> items.sortedByDescending { it.watchedAtMillis }
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

fun filterHistory(items: List<HistoryItem>, filter: String): List<HistoryItem> =
    if (filter.isBlank()) items
    else items.filter { matchesFilter(it.title, filter) || matchesFilter(it.channelName, filter) }

fun filterPlaylistVideos(items: List<PlaylistVideo>, filter: String): List<PlaylistVideo> =
    if (filter.isBlank()) items
    else items.filter { matchesFilter(it.title, filter) }

fun filterPlaylists(items: List<Playlist>, filter: String): List<Playlist> =
    if (filter.isBlank()) items
    else items.filter { matchesFilter(it.name, filter) }
