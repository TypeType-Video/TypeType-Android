package dev.typetype.android.data.account

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY lastUsedAt DESC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE serverId = :serverId ORDER BY lastUsedAt DESC")
    fun observeForServer(serverId: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE serverId = :serverId AND accountId = :accountId")
    suspend fun get(serverId: String, accountId: String): AccountEntity?

    @Query(
        "SELECT sessionGeneration FROM accounts WHERE serverId = :serverId AND accountId = :accountId",
    )
    fun observeSessionGeneration(serverId: String, accountId: String): Flow<Long?>

    @Upsert
    suspend fun upsert(account: AccountEntity)

    @Query(
        "UPDATE accounts SET lastUsedAt = :lastUsedAt " +
            "WHERE serverId = :serverId AND accountId = :accountId",
    )
    suspend fun updateLastUsed(serverId: String, accountId: String, lastUsedAt: Long)

    @Query("DELETE FROM accounts WHERE serverId = :serverId AND accountId = :accountId")
    suspend fun delete(serverId: String, accountId: String)
}
