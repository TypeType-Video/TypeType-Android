package dev.typetype.android.data.library.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.typetype.android.domain.library.WatchLaterItem

@Entity(tableName = "watch_later")
data class WatchLaterEntity(
    @PrimaryKey val url: String,
    val title: String,
    val thumbnailUrl: String,
    val durationSeconds: Long,
    val addedAtMillis: Long,
) {
    fun toDomain(): WatchLaterItem = WatchLaterItem(
        url = url,
        title = title,
        thumbnailUrl = thumbnailUrl,
        durationSeconds = durationSeconds,
        addedAtMillis = addedAtMillis,
    )

    companion object {
        fun fromDomain(item: WatchLaterItem): WatchLaterEntity = WatchLaterEntity(
            url = item.url,
            title = item.title,
            thumbnailUrl = item.thumbnailUrl,
            durationSeconds = item.durationSeconds,
            addedAtMillis = item.addedAtMillis,
        )
    }
}
