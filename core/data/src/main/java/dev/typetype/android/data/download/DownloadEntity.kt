package dev.typetype.android.data.download

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import dev.typetype.android.data.account.AccountEntity

@Entity(
    tableName = "downloads",
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
data class DownloadEntity(
    val serverId: String,
    val accountId: String,
    @ColumnInfo(defaultValue = "0") val sessionGeneration: Long,
    val requestId: String,
    val workId: String,
    val videoUrl: String,
    val title: String,
    val quality: String,
    val serverJobId: String?,
    val systemDownloadId: Long?,
    val fileName: String?,
    val status: String,
    val progressPercent: Int?,
    val stage: String?,
    val errorMessage: String?,
    val cached: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
