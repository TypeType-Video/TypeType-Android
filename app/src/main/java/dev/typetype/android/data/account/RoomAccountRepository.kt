package dev.typetype.android.data.account

import dev.typetype.android.data.network.AccessTokenStore
import dev.typetype.android.data.network.PersistentCookieJar
import dev.typetype.android.data.network.RetrofitFactory
import dev.typetype.android.domain.account.Account
import dev.typetype.android.domain.account.AccountRepository
import dev.typetype.android.domain.server.ServerRepository
import javax.inject.Inject
import javax.inject.Singleton
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class RoomAccountRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val tokenStore: AccessTokenStore,
    private val cookieJar: PersistentCookieJar,
    private val accountScopeStore: AccountScopeStore,
    private val serverRepository: ServerRepository,
    private val retrofitFactory: RetrofitFactory,
) : AccountRepository {
    override fun observeAccounts(): Flow<List<Account>> =
        accountDao.observeAll().map { accounts -> accounts.map(AccountEntity::toDomain) }

    override suspend fun select(serverId: String, accountId: String): Result<Unit> = runCatching {
        val server = requireNotNull(serverRepository.getServer(serverId)) { "Instance not found" }
        requireNotNull(accountDao.get(serverId, accountId)) { "Account not found" }
        val token = tokenStore.getAccessToken(serverId, accountId)
            ?: error("This account needs to sign in again")
        validateSavedSession(serverId, accountId, server.baseUrl, token)
        val previousServerId = serverRepository.observeCurrentServer().first()?.id
        val previousAccountId = accountScopeStore.getCurrentAccountId(serverId)
        try {
            accountScopeStore.setCurrentAccountId(serverId, accountId)
            serverRepository.setCurrentServer(serverId)
        } catch (error: Exception) {
            restoreSelection(serverId, previousAccountId, previousServerId)
            throw error
        }
        accountDao.updateLastUsed(serverId, accountId, System.currentTimeMillis())
    }

    override suspend fun forget(serverId: String, accountId: String): Result<Unit> = runCatching {
        val selectedServer = serverRepository.observeCurrentServer().first()?.id
        val selectedAccount = accountScopeStore.getCurrentAccountId(serverId)
        check(selectedServer != serverId || selectedAccount != accountId) {
            "Switch to another account before forgetting this one"
        }
        tokenStore.removeAccount(serverId, accountId)
        cookieJar.clearAccount(serverId, accountId)
        accountDao.delete(serverId, accountId)
    }

    private suspend fun validateSavedSession(
        serverId: String,
        accountId: String,
        baseUrl: String,
        token: String,
    ) {
        val response = try {
            withContext(Dispatchers.IO) {
                retrofitFactory.createForExplicitToken(baseUrl).me("Bearer $token")
            }
        } catch (_: IOException) {
            return
        }
        when {
            response.code() == 401 || response.code() == 403 -> {
                error("This account needs to sign in again")
            }
            response.isSuccessful -> {
                val profile = response.body() ?: error("The instance returned an empty account")
                check(profile.id == accountId) { "The saved session belongs to another account" }
                val generation = requireNotNull(accountDao.get(serverId, accountId)).sessionGeneration
                accountDao.upsert(AccountEntity.fromProfile(serverId, profile, generation))
            }
        }
    }

    private suspend fun restoreSelection(
        serverId: String,
        previousAccountId: String?,
        previousServerId: String?,
    ) {
        if (previousAccountId == null) {
            accountScopeStore.clearCurrentAccountId(serverId)
        } else {
            accountScopeStore.setCurrentAccountId(serverId, previousAccountId)
        }
        if (previousServerId == null) {
            serverRepository.clearCurrentServer()
        } else {
            serverRepository.setCurrentServer(previousServerId)
        }
    }
}
