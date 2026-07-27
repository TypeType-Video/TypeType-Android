package dev.typetype.android.data.library.sync

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import dev.typetype.android.data.account.AccountEntity

@Entity(
    tableName = "library_sync_state",
    primaryKeys = ["serverId", "accountId", "collection"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["serverId", "accountId"],
            childColumns = ["serverId", "accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("serverId", "accountId")],
)
data class LibrarySyncEntity(
    val serverId: String,
    val accountId: String,
    val collection: String,
    val refreshGeneration: Long,
    val lastAttemptAtMillis: Long,
    val lastSuccessAtMillis: Long?,
    val lastFailureAtMillis: Long?,
    val failureCode: String?,
    val failureStatusCode: Int?,
    val requestId: String?,
)
