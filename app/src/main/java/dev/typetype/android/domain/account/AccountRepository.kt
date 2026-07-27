package dev.typetype.android.domain.account

import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAccounts(): Flow<List<Account>>
    suspend fun select(serverId: String, accountId: String): Result<Unit>
    suspend fun forget(serverId: String, accountId: String): Result<Unit>
}
