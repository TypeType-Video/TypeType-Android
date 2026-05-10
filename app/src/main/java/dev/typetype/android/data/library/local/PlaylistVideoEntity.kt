package dev.typetype.android.data.library.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "playlist_videos",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    primaryKeys = ["playlistId", "id"],
    indices = [Index("playlistId")],
)
data class PlaylistVideoEntity(
    val playlistId: String,
    val id: String,
    val url: String,
    val title: String,
    val thumbnailUrl: String,
    val durationSeconds: Long,
    val position: Int,
    @ColumnInfo(defaultValue = "") val channelName: String = "",
    @ColumnInfo(defaultValue = "") val channelUrl: String = "",
    @ColumnInfo(defaultValue = "") val channelAvatarUrl: String = "",
    @ColumnInfo(defaultValue = "0") val viewCount: Long = 0L,
)
