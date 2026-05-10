package dev.typetype.android.data.library.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_meta")
data class VideoMetaEntity(
    @PrimaryKey val videoUrl: String,
    val channelName: String,
    val channelUrl: String,
    val channelAvatarUrl: String,
    val viewCount: Long,
    val updatedAtMillis: Long,
)
