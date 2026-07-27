package dev.typetype.android.data.library.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchLaterDao {
    @Query(
        "SELECT * FROM watch_later WHERE serverId = :serverId AND accountId = :accountId " +
            "ORDER BY addedAtMillis DESC",
    )
    fun observeAll(serverId: String, accountId: String): Flow<List<WatchLaterEntity>>

    @Query(
        "SELECT * FROM watch_later WHERE serverId = :serverId AND accountId = :accountId " +
            "ORDER BY addedAtMillis DESC",
    )
    suspend fun getAll(serverId: String, accountId: String): List<WatchLaterEntity>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM watch_later WHERE serverId = :serverId " +
            "AND accountId = :accountId AND url = :url)",
    )
    fun observeIsInWatchLater(serverId: String, accountId: String, url: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: WatchLaterEntity)

    @Query(
        "DELETE FROM watch_later WHERE serverId = :serverId " +
            "AND accountId = :accountId AND url = :url",
    )
    suspend fun deleteByUrl(serverId: String, accountId: String, url: String)

    @Query("DELETE FROM watch_later WHERE serverId = :serverId AND accountId = :accountId")
    suspend fun deleteAll(serverId: String, accountId: String)

    @Transaction
    suspend fun replaceAll(serverId: String, accountId: String, items: List<WatchLaterEntity>) {
        deleteAll(serverId, accountId)
        items.forEach { upsert(it) }
    }
}
