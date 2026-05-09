package dev.typetype.android.data.library.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorites ORDER BY favoritedAtMillis DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE videoUrl = :videoUrl)")
    fun observeIsFavorite(videoUrl: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE videoUrl = :videoUrl")
    suspend fun deleteByUrl(videoUrl: String)

    @Query("DELETE FROM favorites")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(favorites: List<FavoriteEntity>) {
        deleteAll()
        favorites.forEach { upsert(it) }
    }
}
