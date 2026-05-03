package dev.typetype.android.feature.library

import dev.typetype.android.domain.library.FavoriteItem
import dev.typetype.android.domain.library.HistoryItem
import dev.typetype.android.domain.library.Playlist
import dev.typetype.android.domain.library.WatchLaterItem

enum class LibraryTab { History, Favorites, WatchLater, Playlists }

data class LibraryState(
    val selectedTab: LibraryTab = LibraryTab.History,
    val isLoading: Boolean = false,
    val history: List<HistoryItem> = emptyList(),
    val favorites: List<FavoriteItem> = emptyList(),
    val watchLater: List<WatchLaterItem> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val errorMessage: String? = null,
)
