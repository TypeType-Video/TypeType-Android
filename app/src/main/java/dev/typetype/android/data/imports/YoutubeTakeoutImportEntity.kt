package dev.typetype.android.data.imports

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import dev.typetype.android.data.account.AccountEntity

@Entity(
    tableName = "youtube_takeout_imports",
    primaryKeys = ["serverId", "accountId", "requestId"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["serverId", "accountId"],
            childColumns = ["serverId", "accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["serverId", "accountId"]),
        Index(value = ["workId"], unique = true),
    ],
)
data class YoutubeTakeoutImportEntity(
    val serverId: String,
    val accountId: String,
    val sessionGeneration: Long,
    val requestId: String,
    val workId: String,
    val documentUri: String,
    val displayName: String,
    val sizeBytes: Long?,
    val serverJobId: String?,
    val status: String,
    val phase: String?,
    val progressPercent: Int?,
    val previewSubscriptions: Int?,
    val previewPlaylists: Int?,
    val previewPlaylistItems: Int?,
    val previewFavorites: Int?,
    val previewWatchLater: Int?,
    val previewHistory: Int?,
    val importedCount: Int?,
    val skippedCount: Int?,
    val failedCount: Int?,
    val warningCount: Int,
    val errorCount: Int,
    val failureCode: String?,
    val failureRequestId: String?,
    val collectionsRefreshed: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
