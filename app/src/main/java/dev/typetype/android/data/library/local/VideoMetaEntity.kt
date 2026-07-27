package dev.typetype.android.data.library.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import dev.typetype.android.data.account.AccountEntity

@Entity(
    tableName = "video_meta",
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
data class VideoMetaEntity(
    val serverId: String,
    val accountId: String,
    val videoUrl: String,
    val channelName: String,
    val channelUrl: String,
    val channelAvatarUrl: String,
    val viewCount: Long,
    val updatedAtMillis: Long,
)
