package dev.typetype.android.data.download

import kotlinx.serialization.Serializable

@Serializable
data class StoredDownloadEntry(
    val downloadId: Long,
    val title: String,
    val fileName: String,
    val createdAtMillis: Long,
)
