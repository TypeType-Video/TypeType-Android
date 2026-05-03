package dev.typetype.android.feature.player

sealed interface PlayerEvent {
    data class FavoriteAdded(val title: String) : PlayerEvent
    data object FavoriteRemoved : PlayerEvent
    data class WatchLaterAdded(val title: String) : PlayerEvent
    data object WatchLaterRemoved : PlayerEvent
    data class ActionFailed(val message: String) : PlayerEvent
}
