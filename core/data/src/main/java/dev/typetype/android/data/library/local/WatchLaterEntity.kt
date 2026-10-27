package dev.typetype.android.data.library.local

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import dev.typetype.android.data.account.AccountEntity
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.domain.library.WatchLaterItem

@Entity(
    tableName = "watch_later",
    primaryKeys = ["serverId", "accountId", "url"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["serverId", "accountId"],
            childColumns = ["serverId", "accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["serverId", "accountId"])],
)
data class WatchLaterEntity(
    val serverId: String,
    val accountId: String,
    val url: String,
    val title: String,
    val thumbnailUrl: String,
    val durationSeconds: Long,
    val addedAtMillis: Long,
    @ColumnInfo(defaultValue = "") val channelName: String,
    @ColumnInfo(defaultValue = "") val channelUrl: String,
    @ColumnInfo(defaultValue = "") val channelAvatarUrl: String,
    @ColumnInfo(defaultValue = "0") val viewCount: Long,
) {
    fun toDomain(): WatchLaterItem = WatchLaterItem(
        url = url,
        title = title,
        thumbnailUrl = thumbnailUrl,
        durationSeconds = durationSeconds,
        addedAtMillis = addedAtMillis,
        channelName = channelName,
        channelUrl = channelUrl,
        channelAvatarUrl = channelAvatarUrl,
        viewCount = viewCount,
    )

    companion object {
        fun fromDomain(scope: AccountScope, item: WatchLaterItem): WatchLaterEntity = WatchLaterEntity(
            serverId = scope.serverId,
            accountId = scope.accountId,
            url = item.url,
            title = item.title,
            thumbnailUrl = item.thumbnailUrl,
            durationSeconds = item.durationSeconds,
            addedAtMillis = item.addedAtMillis,
            channelName = item.channelName,
            channelUrl = item.channelUrl,
            channelAvatarUrl = item.channelAvatarUrl,
            viewCount = item.viewCount,
        )
    }
}
