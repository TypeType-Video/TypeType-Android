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
    val fromMillis: Long? = null,
    val toMillis: Long? = null,
) {
    val hasRemoteFilter: Boolean
        get() = search.isNotBlank() || fromMillis != null || toMillis != null

    init {
        require(fromMillis == null || toMillis == null || fromMillis < toMillis)
    }
}

data class HistoryDateRange(
    val fromMillis: Long,
    val toMillis: Long,
) {
    init {
        require(fromMillis < toMillis)
    }
}
