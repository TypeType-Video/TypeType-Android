package dev.typetype.android.data.library.sync

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProgressOutboxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ProgressOutboxEntity)

    @Query(
        "SELECT * FROM progress_outbox WHERE serverId = :serverId AND accountId = :accountId " +
            "AND sessionGeneration = :generation ORDER BY updatedAtMillis LIMIT :limit",
    )
    suspend fun pending(
        serverId: String,
        accountId: String,
        generation: Long,
        limit: Int,
    ): List<ProgressOutboxEntity>

    @Query(
        "SELECT * FROM progress_outbox WHERE serverId = :serverId AND accountId = :accountId " +
            "AND videoUrl = :videoUrl LIMIT 1",
    )
    suspend fun get(serverId: String, accountId: String, videoUrl: String): ProgressOutboxEntity?

    @Query(
        "DELETE FROM progress_outbox WHERE serverId = :serverId AND accountId = :accountId " +
            "AND videoUrl = :videoUrl AND sessionGeneration = :generation " +
            "AND updatedAtMillis = :updatedAtMillis",
    )
    suspend fun deleteIfUnchanged(
        serverId: String,
        accountId: String,
        videoUrl: String,
        generation: Long,
        updatedAtMillis: Long,
    )

    @Query(
        "DELETE FROM progress_outbox WHERE serverId = :serverId AND accountId = :accountId " +
            "AND sessionGeneration != :generation",
    )
    suspend fun deleteStale(serverId: String, accountId: String, generation: Long)

    @Query(
        "DELETE FROM progress_outbox WHERE serverId = :serverId AND accountId = :accountId " +
            "AND sessionGeneration = :generation",
    )
    suspend fun deleteGeneration(serverId: String, accountId: String, generation: Long)

    @Query("DELETE FROM progress_outbox WHERE serverId = :serverId AND accountId = :accountId")
    suspend fun deleteAllForScope(serverId: String, accountId: String)
}
