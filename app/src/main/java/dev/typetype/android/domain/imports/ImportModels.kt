package dev.typetype.android.domain.imports

data class ImportDocument(
    val uri: String,
    val displayName: String,
    val sizeBytes: Long?,
    val mediaType: String?,
)

data class PipePipeRestoreSummary(
    val history: Int,
    val subscriptions: Int,
    val playlists: Int,
    val playlistVideos: Int,
    val progress: Int,
    val searchHistory: Int,
    val historyMinWatchedAt: Long?,
    val historyMaxWatchedAt: Long?,
)

enum class TypeTypeBackupCategory(val wireName: String) {
    Subscriptions("subscriptions"),
    History("history"),
    Playlists("playlists"),
    WatchLater("watchLater"),
    Favorites("favorites"),
    Progress("progress"),
    SearchHistory("searchHistory"),
    SavedPlaylists("savedPlaylists"),
    Settings("settings"),
    ContentFilters("contentFilters"),
}

data class TypeTypeRestoreSummary(
    val restored: Map<TypeTypeBackupCategory, Int>,
) {
    val total: Int
        get() = restored.values.sum()
}
