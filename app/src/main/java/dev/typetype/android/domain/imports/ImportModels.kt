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
