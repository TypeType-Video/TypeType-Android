package dev.typetype.android.feature.library

sealed interface LibraryAction {
    data class OnTabSelect(val tab: LibraryTab) : LibraryAction
    data object OnRefresh : LibraryAction
}
