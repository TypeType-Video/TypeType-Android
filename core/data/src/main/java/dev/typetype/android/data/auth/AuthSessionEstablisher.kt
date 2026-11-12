package dev.typetype.android.data.auth

import dev.typetype.android.data.account.AccountDao
import dev.typetype.android.data.account.AccountEntity
import dev.typetype.android.data.account.AccountScopeStore
import dev.typetype.android.data.network.AccessTokenStore
import dev.typetype.android.data.network.PersistentCookieJar
import dev.typetype.android.data.network.TypeTypeApi
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.server.ServerRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AuthSessionEstablisher @Inject constructor(
    private val serverRepository: ServerRepository,
    private val tokenStore: AccessTokenStore,
    private val cookieJar: PersistentCookieJar,
    private val accountDao: AccountDao,
    private val accountScopeStore: AccountScopeStore,
) {
    suspend fun establish(
        serverId: String,
        api: TypeTypeApi,
        token: String,
        expectedAccountId: String? = null,
    ) {
        val profileResponse = withContext(Dispatchers.IO) { api.me("Bearer $token") }
        profileResponse.requireSuccessfulResponse()
        val profile = profileResponse.body() ?: error("The instance returned an empty account")
        requireExpectedAccount(expectedAccountId, profile.id)
        val previousGeneration = accountDao.get(serverId, profile.id)?.sessionGeneration ?: 0L
        val generation = maxOf(System.currentTimeMillis(), previousGeneration + 1L)
        accountDao.upsert(AccountEntity.fromProfile(serverId, profile, generation))
        tokenStore.setAuthenticatedAccessToken(serverId, profile.id, token)
        cookieJar.completeAuthentication(serverId, profile.id)
        accountScopeStore.setCurrentAccountId(serverId, profile.id)
        serverRepository.setCurrentServer(serverId)
    }
}
