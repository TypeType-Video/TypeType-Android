package dev.typetype.android.feature.search

sealed interface SearchAction {
    data class OnQueryChange(val query: String) : SearchAction
    data object OnSearch : SearchAction
    data object OnClearQuery : SearchAction
}
