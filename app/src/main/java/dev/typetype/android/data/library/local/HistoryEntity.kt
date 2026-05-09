package dev.typetype.android.data.library.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.typetype.android.domain.library.HistoryItem

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val thumbnailUrl: String,
    val channelName: String,
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
        durationSeconds = durationSeconds,
        progressSeconds = progressSeconds,
        watchedAtMillis = watchedAtMillis,
    )

    companion object {
        fun fromDomain(item: HistoryItem): HistoryEntity = HistoryEntity(
            id = item.id,
            url = item.url,
            title = item.title,
            thumbnailUrl = item.thumbnailUrl,
            channelName = item.channelName,
            durationSeconds = item.durationSeconds,
            progressSeconds = item.progressSeconds,
            watchedAtMillis = item.watchedAtMillis,
        )
    }
}
