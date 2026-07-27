package dev.typetype.android.data.playback

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.ColumnInfo
import dev.typetype.android.data.account.AccountEntity
import dev.typetype.android.domain.playback.PlaybackQueueEntry

@Entity(
    tableName = "playback_queue",
    primaryKeys = ["serverId", "accountId", "position"],
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
data class PlaybackQueueEntity(
    val serverId: String,
    val accountId: String,
    val position: Int,
    val currentIndex: Int,
    val queueTitle: String,
    @ColumnInfo(defaultValue = "'Off'")
    val repeatMode: String,
    val videoUrl: String,
    val title: String,
    val thumbnailUrl: String,
    val durationSeconds: Long,
    val channelName: String,
    val updatedAtMillis: Long,
) {
    fun toEntry(): PlaybackQueueEntry = PlaybackQueueEntry(
        videoUrl = videoUrl,
        title = title,
        thumbnailUrl = thumbnailUrl,
        durationSeconds = durationSeconds,
        channelName = channelName,
    )
}
