package dev.typetype.android.data.library.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY watchedAtMillis DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT progressSeconds FROM history WHERE url = :url LIMIT 1")
    suspend fun getProgressSeconds(url: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: HistoryEntity)

    @Query("UPDATE history SET progressSeconds = :seconds, watchedAtMillis = :watchedAtMillis WHERE url = :url")
    suspend fun updateProgress(url: String, seconds: Long, watchedAtMillis: Long)

    @Query("DELETE FROM history")
    suspend fun deleteAll()

    @Query("DELETE FROM history WHERE url = :url")
    suspend fun deleteByUrl(url: String)

    @Transaction
    suspend fun replaceAll(items: List<HistoryEntity>) {
        deleteAll()
        items.forEach { upsert(it) }
    }
}
