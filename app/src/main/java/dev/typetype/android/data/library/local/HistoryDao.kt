package dev.typetype.android.data.library.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query(
        "SELECT * FROM history WHERE serverId = :serverId AND accountId = :accountId " +
        "AND (:search = '' OR instr(lower(title), lower(:search)) > 0 " +
        "OR instr(lower(channelName), lower(:search)) > 0) " +
            "AND (:fromMillis IS NULL OR watchedAtMillis >= :fromMillis) " +
            "AND (:toMillis IS NULL OR watchedAtMillis < :toMillis) " +
            "ORDER BY " +
            "CASE WHEN :orderKey = 0 THEN watchedAtMillis END DESC, " +
            "CASE WHEN :orderKey = 1 THEN watchedAtMillis END ASC, " +
            "CASE WHEN :orderKey = 2 THEN title END COLLATE NOCASE ASC, " +
            "CASE WHEN :orderKey = 3 THEN title END COLLATE NOCASE DESC, id DESC",
    )
    fun pagingSource(
        serverId: String,
        accountId: String,
        search: String,
        orderKey: Int,
        fromMillis: Long?,
        toMillis: Long?,
    ): PagingSource<Int, HistoryEntity>

    @Query(
        "DELETE FROM history WHERE serverId = :serverId AND accountId = :accountId " +
            "AND (:search = '' OR instr(lower(title), lower(:search)) > 0 " +
            "OR instr(lower(channelName), lower(:search)) > 0) " +
            "AND (:fromMillis IS NULL OR watchedAtMillis >= :fromMillis) " +
            "AND (:toMillis IS NULL OR watchedAtMillis < :toMillis)",
    )
    suspend fun deleteMatching(
        serverId: String,
        accountId: String,
        search: String,
        fromMillis: Long?,
        toMillis: Long?,
    )

    @Query("SELECT COUNT(*) FROM history WHERE serverId = :serverId AND accountId = :accountId")
    fun observeCount(serverId: String, accountId: String): Flow<Int>

    @Query(
        "SELECT url FROM history WHERE serverId = :serverId AND accountId = :accountId " +
            "AND durationSeconds > 0 AND progressSeconds * 10 >= durationSeconds * 9",
    )
    fun observeWatchedUrls(serverId: String, accountId: String): Flow<List<String>>

    @Query(
        "SELECT * FROM history WHERE serverId = :serverId AND accountId = :accountId " +
            "AND progressSeconds > 0 AND durationSeconds > 0 " +
            "AND ((durationSeconds > 60 AND progressSeconds < durationSeconds - 60) " +
            "OR (durationSeconds <= 60 AND progressSeconds * 10 < durationSeconds * 9)) " +
            "ORDER BY watchedAtMillis DESC, id DESC LIMIT :limit",
    )
    fun observeContinueWatching(
        serverId: String,
        accountId: String,
        limit: Int,
    ): Flow<List<HistoryEntity>>

    @Query(
        "SELECT progressSeconds FROM history WHERE serverId = :serverId " +
            "AND accountId = :accountId AND url = :url LIMIT 1",
    )
    suspend fun getProgressSeconds(serverId: String, accountId: String, url: String): Long?

    @Query(
        "SELECT id FROM history WHERE serverId = :serverId " +
            "AND accountId = :accountId AND url = :url LIMIT 1",
    )
    suspend fun getIdByUrl(serverId: String, accountId: String, url: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: HistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<HistoryEntity>)

    @Query(
        "UPDATE history SET progressSeconds = :seconds, watchedAtMillis = :watchedAtMillis " +
            "WHERE serverId = :serverId AND accountId = :accountId AND url = :url",
    )
    suspend fun updateProgress(
        serverId: String,
        accountId: String,
        url: String,
        seconds: Long,
        watchedAtMillis: Long,
    )

    @Query("DELETE FROM history WHERE serverId = :serverId AND accountId = :accountId")
    suspend fun deleteAll(serverId: String, accountId: String)

    @Query(
        "DELETE FROM history WHERE serverId = :serverId " +
            "AND accountId = :accountId AND url = :url",
    )
    suspend fun deleteByUrl(serverId: String, accountId: String, url: String)

    @Transaction
    suspend fun replaceAll(serverId: String, accountId: String, items: List<HistoryEntity>) {
        deleteAll(serverId, accountId)
        items.forEach { upsert(it) }
    }
}
