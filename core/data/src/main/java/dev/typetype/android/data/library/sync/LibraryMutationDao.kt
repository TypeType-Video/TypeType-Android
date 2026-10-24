package dev.typetype.android.data.library.sync

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryMutationDao {
    @Query(
        "SELECT * FROM library_mutation_outbox WHERE serverId = :serverId " +
            "AND accountId = :accountId ORDER BY updatedAtMillis DESC",
    )
    fun observe(serverId: String, accountId: String): Flow<List<LibraryMutationEntity>>

    @Query(
        "SELECT * FROM library_mutation_outbox WHERE serverId = :serverId " +
            "AND accountId = :accountId AND sessionGeneration = :generation " +
            "AND state = 'pending' ORDER BY updatedAtMillis LIMIT :limit",
    )
    suspend fun pending(
        serverId: String,
        accountId: String,
        generation: Long,
        limit: Int,
    ): List<LibraryMutationEntity>

    @Query(
        "SELECT * FROM library_mutation_outbox WHERE serverId = :serverId " +
            "AND accountId = :accountId AND mutationKey = :mutationKey LIMIT 1",
    )
    suspend fun get(serverId: String, accountId: String, mutationKey: String): LibraryMutationEntity?

    @Query(
        "SELECT * FROM library_mutation_outbox WHERE serverId = :serverId " +
            "AND accountId = :accountId AND collection = :collection",
    )
    suspend fun forCollection(
        serverId: String,
        accountId: String,
        collection: String,
    ): List<LibraryMutationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LibraryMutationEntity)

    @Query(
        "DELETE FROM library_mutation_outbox WHERE serverId = :serverId " +
            "AND accountId = :accountId AND mutationKey = :mutationKey " +
            "AND mutationVersion = :version",
    )
    suspend fun deleteIfCurrent(
        serverId: String,
        accountId: String,
        mutationKey: String,
        version: Long,
    ): Int

    @Query(
        "UPDATE library_mutation_outbox SET state = :state, lastAttemptAtMillis = :attemptedAt, " +
            "attemptCount = attemptCount + 1, failureCode = :code, " +
            "failureStatusCode = :status, requestId = :requestId " +
            "WHERE serverId = :serverId AND accountId = :accountId " +
            "AND mutationKey = :mutationKey AND mutationVersion = :version",
    )
    suspend fun recordAttempt(
        serverId: String,
        accountId: String,
        mutationKey: String,
        version: Long,
        state: String,
        attemptedAt: Long,
        code: String?,
        status: Int?,
        requestId: String?,
    ): Int

    @Query(
        "UPDATE library_mutation_outbox SET state = 'pending', failureCode = NULL, " +
            "failureStatusCode = NULL, requestId = NULL, updatedAtMillis = :updatedAt " +
            "WHERE serverId = :serverId AND accountId = :accountId " +
            "AND collection = :collection AND state = 'failed'",
    )
    suspend fun retryFailed(serverId: String, accountId: String, collection: String, updatedAt: Long): Int

    @Query(
        "DELETE FROM library_mutation_outbox WHERE serverId = :serverId AND accountId = :accountId " +
            "AND sessionGeneration != :generation",
    )
    suspend fun deleteStale(serverId: String, accountId: String, generation: Long)

    @Transaction
    suspend fun nextVersion(serverId: String, accountId: String, mutationKey: String): Long =
        (get(serverId, accountId, mutationKey)?.mutationVersion ?: 0L) + 1L
}
