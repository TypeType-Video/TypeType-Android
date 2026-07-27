package dev.typetype.android.domain.library

enum class HistoryOrder(val storageKey: Int) {
    Recent(0),
    Oldest(1),
    TitleAscending(2),
    TitleDescending(3),
}

data class HistoryQuery(
    val search: String = "",
    val order: HistoryOrder = HistoryOrder.Recent,
)
