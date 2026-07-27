package dev.typetype.android.data.library.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import dev.typetype.android.data.account.AccountEntity
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.domain.library.HistoryItem

@Entity(
    tableName = "history",
    primaryKeys = ["serverId", "accountId", "id"],
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
        Index(value = ["serverId", "accountId", "url"]),
    ],
)
data class HistoryEntity(
    val serverId: String,
    val accountId: String,
    val id: String,
    val url: String,
    val title: String,
    val thumbnailUrl: String,
    val channelName: String,
    @ColumnInfo(defaultValue = "") val channelUrl: String = "",
    @ColumnInfo(defaultValue = "") val channelAvatarUrl: String = "",
    val durationSeconds: Long,
    val progressSeconds: Long,
    val watchedAtMillis: Long,
) {
    fun toDomain(): HistoryItem = HistoryItem(
        id = id,
        url = url,
        title = title,
        thumbnailUrl = thumbnailUrl,
        channelName = channelName,
        channelUrl = channelUrl,
        channelAvatarUrl = channelAvatarUrl,
        durationSeconds = durationSeconds,
        progressSeconds = progressSeconds,
        watchedAtMillis = watchedAtMillis,
    )

    companion object {
        fun fromDomain(scope: AccountScope, item: HistoryItem): HistoryEntity = HistoryEntity(
            serverId = scope.serverId,
            accountId = scope.accountId,
            id = item.id,
            url = item.url,
            title = item.title,
            thumbnailUrl = item.thumbnailUrl,
            channelName = item.channelName,
            channelUrl = item.channelUrl,
            channelAvatarUrl = item.channelAvatarUrl,
            durationSeconds = item.durationSeconds,
            progressSeconds = item.progressSeconds,
            watchedAtMillis = item.watchedAtMillis,
        )
    }
}
