package dev.typetype.android.data.library.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.typetype.android.domain.library.FavoriteItem

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val videoUrl: String,
    val favoritedAtMillis: Long,
) {
    fun toDomain(): FavoriteItem = FavoriteItem(
        videoUrl = videoUrl,
        favoritedAtMillis = favoritedAtMillis,
    )

    companion object {
        fun fromDomain(item: FavoriteItem): FavoriteEntity = FavoriteEntity(
            videoUrl = item.videoUrl,
            favoritedAtMillis = item.favoritedAtMillis,
        )
    }
}
