package dev.typetype.android.data.library.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.typetype.android.data.account.AccountEntity
import dev.typetype.android.data.account.AccountScope

@Entity(
    tableName = "playlists",
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
        Index(value = ["serverId", "accountId", "id"], unique = true),
    ],
)
data class PlaylistEntity(
    @PrimaryKey val cacheKey: String,
    val serverId: String,
    val accountId: String,
    val id: String,
    val name: String,
    val description: String,
    val createdAtMillis: Long,
    @ColumnInfo(defaultValue = "0") val videoCount: Int = 0,
) {
    companion object {
        fun cacheKey(scope: AccountScope, playlistId: String): String =
            "${scope.serverId.length}:${scope.serverId}${scope.accountId.length}:${scope.accountId}$playlistId"
    }
}
