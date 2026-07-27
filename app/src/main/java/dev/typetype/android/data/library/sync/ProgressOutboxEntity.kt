package dev.typetype.android.data.library.sync

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import dev.typetype.android.data.account.AccountEntity

@Entity(
    tableName = "progress_outbox",
    primaryKeys = ["serverId", "accountId", "videoUrl"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["serverId", "accountId"],
            childColumns = ["serverId", "accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["serverId", "accountId", "sessionGeneration"])],
)
data class ProgressOutboxEntity(
    val serverId: String,
    val accountId: String,
    val videoUrl: String,
    val positionMillis: Long,
    val sessionGeneration: Long,
    val updatedAtMillis: Long,
)
