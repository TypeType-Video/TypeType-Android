package dev.typetype.android.data.subscriptions

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Query(
        "SELECT * FROM subscriptions WHERE serverId = :serverId AND accountId = :accountId " +
            "ORDER BY subscribedAtMillis DESC",
    )
    fun observe(serverId: String, accountId: String): Flow<List<SubscriptionEntity>>

    @Query(
        "SELECT * FROM subscriptions WHERE serverId = :serverId AND accountId = :accountId " +
            "ORDER BY subscribedAtMillis DESC",
    )
    suspend fun getAll(serverId: String, accountId: String): List<SubscriptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SubscriptionEntity)

    @Query(
        "DELETE FROM subscriptions WHERE serverId = :serverId AND accountId = :accountId " +
            "AND channelUrl = :channelUrl",
    )
    suspend fun delete(serverId: String, accountId: String, channelUrl: String)

    @Query("DELETE FROM subscriptions WHERE serverId = :serverId AND accountId = :accountId")
    suspend fun deleteAll(serverId: String, accountId: String)

    @Transaction
    suspend fun replaceAll(serverId: String, accountId: String, rows: List<SubscriptionEntity>) {
        deleteAll(serverId, accountId)
        rows.forEach { upsert(it) }
    }
}
