package dev.typetype.android.domain.imports

data class YoutubeTakeoutCategoryCounts(
    val subscriptions: Int,
    val playlists: Int,
    val playlistItems: Int,
    val favorites: Int,
    val watchLater: Int,
    val history: Int,
) {
    val total: Int
        get() = subscriptions + playlists + playlistItems + favorites + watchLater + history
}

enum class YoutubeTakeoutImportStatus {
    Queued,
    Uploading,
    Parsing,
    Importing,
    Completed,
    Failed,
    Cancelled,
}

data class YoutubeTakeoutImportItem(
    val requestId: String,
    val displayName: String,
    val sizeBytes: Long?,
    val status: YoutubeTakeoutImportStatus,
    val progressPercent: Int?,
    val preview: YoutubeTakeoutCategoryCounts?,
    val importedCount: Int?,
    val skippedCount: Int?,
    val failedCount: Int?,
    val warningCount: Int,
    val errorCount: Int,
    val failureCode: String?,
    val failureRequestId: String?,
    val needsCollectionRefresh: Boolean,
    val createdAtMillis: Long,
)
