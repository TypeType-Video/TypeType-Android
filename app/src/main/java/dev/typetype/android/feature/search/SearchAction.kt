package dev.typetype.android.feature.search

sealed interface SearchAction {
    data class OnQueryChange(val query: String) : SearchAction
    data object OnSearch : SearchAction
    data class OnSuggestionClick(val query: String) : SearchAction
    data object OnClearQuery : SearchAction
    data class OnContentFilterSelect(val value: String?) : SearchAction
    data class OnSortFilterSelect(val value: String?) : SearchAction
    data object OnLoadMore : SearchAction
    data class OnDeleteHistoryEntry(val query: String) : SearchAction
    data class OnHistoryEntryClick(val query: String) : SearchAction
}
