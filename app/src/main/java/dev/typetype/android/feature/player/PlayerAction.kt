package dev.typetype.android.feature.player

sealed interface PlayerAction {
    data object OnToggleFavorite : PlayerAction
    data object OnToggleWatchLater : PlayerAction
    data class OnSaveProgress(val positionMillis: Long) : PlayerAction
}
