package dev.typetype.android.data.library.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchLaterDao {
    @Query("SELECT * FROM watch_later ORDER BY addedAtMillis DESC")
    fun observeAll(): Flow<List<WatchLaterEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watch_later WHERE url = :url)")
    fun observeIsInWatchLater(url: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: WatchLaterEntity)

    @Query("DELETE FROM watch_later WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    @Query("DELETE FROM watch_later")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(items: List<WatchLaterEntity>) {
        deleteAll()
        items.forEach { upsert(it) }
    }
}
