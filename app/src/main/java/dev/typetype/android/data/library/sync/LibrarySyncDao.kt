package dev.typetype.android.data.library.sync

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LibrarySyncDao {
    @Query(
        "SELECT * FROM library_sync_state WHERE serverId = :serverId " +
            "AND accountId = :accountId",
    )
    fun observe(serverId: String, accountId: String): Flow<List<LibrarySyncEntity>>

    @Query(
        "SELECT * FROM library_sync_state WHERE serverId = :serverId " +
            "AND accountId = :accountId AND collection = :collection LIMIT 1",
    )
    suspend fun get(serverId: String, accountId: String, collection: String): LibrarySyncEntity?

    @Query(
        "SELECT EXISTS(SELECT 1 FROM library_sync_state WHERE serverId = :serverId " +
            "AND accountId = :accountId AND collection = :collection " +
            "AND refreshGeneration = :generation)",
    )
    suspend fun isCurrent(
        serverId: String,
        accountId: String,
        collection: String,
        generation: Long,
    ): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LibrarySyncEntity)

    @Transaction
    suspend fun begin(
        serverId: String,
        accountId: String,
        collection: String,
        attemptedAtMillis: Long,
    ): Long {
        val current = get(serverId, accountId, collection)
        val generation = (current?.refreshGeneration ?: 0L) + 1L
        upsert(
            current?.copy(
                refreshGeneration = generation,
                lastAttemptAtMillis = attemptedAtMillis,
            ) ?: LibrarySyncEntity(
                serverId = serverId,
                accountId = accountId,
                collection = collection,
                refreshGeneration = generation,
                lastAttemptAtMillis = attemptedAtMillis,
                lastSuccessAtMillis = null,
                lastFailureAtMillis = null,
                failureCode = null,
                failureStatusCode = null,
                requestId = null,
            ),
        )
        return generation
    }

    @Query(
        "UPDATE library_sync_state SET lastSuccessAtMillis = :completedAtMillis, " +
            "lastFailureAtMillis = NULL, failureCode = NULL, failureStatusCode = NULL, " +
            "requestId = NULL WHERE serverId = :serverId AND accountId = :accountId " +
            "AND collection = :collection AND refreshGeneration = :generation",
    )
    suspend fun completeSuccess(
        serverId: String,
        accountId: String,
        collection: String,
        generation: Long,
        completedAtMillis: Long,
    ): Int

    @Query(
        "UPDATE library_sync_state SET lastFailureAtMillis = :completedAtMillis, " +
            "failureCode = :failureCode, failureStatusCode = :failureStatusCode, " +
            "requestId = :requestId WHERE serverId = :serverId AND accountId = :accountId " +
            "AND collection = :collection AND refreshGeneration = :generation",
    )
    suspend fun completeFailure(
        serverId: String,
        accountId: String,
        collection: String,
        generation: Long,
        completedAtMillis: Long,
        failureCode: String?,
        failureStatusCode: Int?,
        requestId: String?,
    ): Int
}
