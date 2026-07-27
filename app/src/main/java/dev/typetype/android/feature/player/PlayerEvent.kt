package dev.typetype.android.feature.player

sealed interface PlayerEvent {
    data class FavoriteAdded(val title: String) : PlayerEvent
    data object FavoriteRemoved : PlayerEvent
    data class WatchLaterAdded(val title: String) : PlayerEvent
    data object WatchLaterRemoved : PlayerEvent
    data class AddedToPlaylist(val playlistName: String) : PlayerEvent
    data class DownloadQueued(val cached: Boolean) : PlayerEvent
    data class DownloadEnqueued(val fileName: String) : PlayerEvent
    data object DownloadFailed : PlayerEvent
    data object ActionFailed : PlayerEvent
}
