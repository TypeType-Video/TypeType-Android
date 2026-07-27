package dev.typetype.android.data.library.local

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import dev.typetype.android.data.account.AccountEntity
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.domain.library.FavoriteItem

@Entity(
    tableName = "favorites",
    primaryKeys = ["serverId", "accountId", "videoUrl"],
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
data class FavoriteEntity(
    val serverId: String,
    val accountId: String,
    val videoUrl: String,
    val favoritedAtMillis: Long,
    @ColumnInfo(defaultValue = "") val title: String,
    @ColumnInfo(defaultValue = "") val thumbnailUrl: String,
    @ColumnInfo(defaultValue = "0") val durationSeconds: Long,
    @ColumnInfo(defaultValue = "") val channelName: String,
    @ColumnInfo(defaultValue = "") val channelUrl: String,
    @ColumnInfo(defaultValue = "") val channelAvatarUrl: String,
    @ColumnInfo(defaultValue = "0") val viewCount: Long,
) {
    fun toDomain(): FavoriteItem = FavoriteItem(
        videoUrl = videoUrl,
        favoritedAtMillis = favoritedAtMillis,
        title = title,
        thumbnailUrl = thumbnailUrl,
        durationSeconds = durationSeconds,
        channelName = channelName,
        channelUrl = channelUrl,
        channelAvatarUrl = channelAvatarUrl,
        viewCount = viewCount,
    )

    companion object {
        fun fromDomain(scope: AccountScope, item: FavoriteItem): FavoriteEntity = FavoriteEntity(
            serverId = scope.serverId,
            accountId = scope.accountId,
            videoUrl = item.videoUrl,
            favoritedAtMillis = item.favoritedAtMillis,
            title = item.title,
            thumbnailUrl = item.thumbnailUrl,
            durationSeconds = item.durationSeconds,
            channelName = item.channelName,
            channelUrl = item.channelUrl,
            channelAvatarUrl = item.channelAvatarUrl,
            viewCount = item.viewCount,
        )
    }
}
