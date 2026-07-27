package dev.typetype.android.domain.library

enum class LibraryCollection(val storageKey: String) {
    History("history"),
    Favorites("favorites"),
    WatchLater("watch_later"),
    Playlists("playlists"),
    SavedPlaylists("saved_playlists"),
    Subscriptions("subscriptions"),
}

data class LibraryCollectionSyncState(
    val collection: LibraryCollection,
    val lastAttemptAtMillis: Long,
    val lastSuccessAtMillis: Long?,
    val lastFailureAtMillis: Long?,
    val failureCode: String?,
    val failureStatusCode: Int?,
    val requestId: String?,
    val pendingWriteCount: Int = 0,
    val failedWriteCount: Int = 0,
    val writeFailureCode: String? = null,
    val writeFailureStatusCode: Int? = null,
    val writeRequestId: String? = null,
)
