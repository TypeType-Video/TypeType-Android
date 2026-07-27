package dev.typetype.android.data.library.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {
    @Query(
        "SELECT * FROM favorites WHERE serverId = :serverId AND accountId = :accountId " +
            "ORDER BY favoritedAtMillis DESC",
    )
    fun observeAll(serverId: String, accountId: String): Flow<List<FavoriteEntity>>

    @Query(
        "SELECT * FROM favorites WHERE serverId = :serverId AND accountId = :accountId " +
            "ORDER BY favoritedAtMillis DESC",
    )
    suspend fun getAll(serverId: String, accountId: String): List<FavoriteEntity>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM favorites WHERE serverId = :serverId " +
            "AND accountId = :accountId AND videoUrl = :videoUrl)",
    )
    fun observeIsFavorite(serverId: String, accountId: String, videoUrl: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(favorite: FavoriteEntity)

    @Query(
        "DELETE FROM favorites WHERE serverId = :serverId " +
            "AND accountId = :accountId AND videoUrl = :videoUrl",
    )
    suspend fun deleteByUrl(serverId: String, accountId: String, videoUrl: String)

    @Query("DELETE FROM favorites WHERE serverId = :serverId AND accountId = :accountId")
    suspend fun deleteAll(serverId: String, accountId: String)

    @Transaction
    suspend fun replaceAll(serverId: String, accountId: String, favorites: List<FavoriteEntity>) {
        deleteAll(serverId, accountId)
        favorites.forEach { upsert(it) }
    }
}
