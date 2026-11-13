package dev.typetype.android.data.auth

import dev.typetype.android.core.error.suspendRunCatching
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.AccessTokenStore
import dev.typetype.android.data.network.PersistentCookieJar
import dev.typetype.android.data.network.RetrofitFactory
import dev.typetype.android.data.network.TypeTypeApi
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.AccountProfileDto
import dev.typetype.android.data.network.dto.ProfileNameRequest
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.account.AccountRepository
import dev.typetype.android.domain.profiles.AccountProfile
import dev.typetype.android.domain.profiles.AccountProfiles
import dev.typetype.android.domain.profiles.AccountProfilesRepository
import dev.typetype.android.domain.server.ServerRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class RemoteAccountProfilesRepository @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
    private val serverRepository: ServerRepository,
    private val retrofitFactory: RetrofitFactory,
    private val tokenStore: AccessTokenStore,
    private val cookieJar: PersistentCookieJar,
    private val sessionEstablisher: AuthSessionEstablisher,
    private val accountRepository: AccountRepository,
) : AccountProfilesRepository {
    override suspend fun profiles(): Result<AccountProfiles> = call { api ->
        val response = api.accountProfiles()
        response.requireSuccessfulResponse()
        val body = response.body() ?: error("Empty profiles body")
        AccountProfiles(
            profiles = body.profiles.map(AccountProfileDto::toDomain),
            activeProfileId = body.activeProfileId,
            defaultProfileId = body.defaultProfileId,
        )
    }

    override suspend fun create(name: String): Result<Unit> = call { api ->
        api.createAccountProfile(ProfileNameRequest(name.trim())).requireSuccessfulResponse()
    }

    override suspend fun rename(profileId: String, name: String): Result<Unit> = call { api ->
        api.renameAccountProfile(profileId, ProfileNameRequest(name.trim())).requireSuccessfulResponse()
    }

    override suspend fun setDefault(profileId: String): Result<Unit> = call { api ->
        api.setDefaultAccountProfile(profileId).requireSuccessfulResponse()
    }

    override suspend fun delete(profileId: String): Result<Unit> {
        val scope = suspendRunCatching { activeAccountScope.require() }.getOrElse { return Result.failure(it) }
        return call { api -> api.deleteAccountProfile(profileId).requireSuccessfulResponse() }
            .onSuccess { accountRepository.forget(scope.serverId, profileId) }
    }

    // A profile is its own server user: it signs in as a separate app account, so its token, refresh cookie and
    // cached data never mix with the profile it came from.
    override suspend fun switchTo(profileId: String): Result<Unit> {
        val scope = suspendRunCatching { activeAccountScope.require() }.getOrElse { return Result.failure(it) }
        val server = serverRepository.getServer(scope.serverId) ?: return Result.failure(IllegalStateException("Instance not found"))
        profiles().onFailure { return Result.failure(it) }
        return suspendRunCatching {
            val token = tokenStore.getAccessToken(scope.serverId, scope.accountId)
                ?: error("This account needs to sign in again")
            cookieJar.beginAuthentication(scope.serverId, server.baseUrl)
            val api = retrofitFactory.createWithoutAutomaticAuthentication(server.baseUrl)
            val response = withContext(Dispatchers.IO) { api.switchAccountProfile("Bearer $token", profileId) }
            response.requireSuccessfulResponse()
            val accessToken = response.body()?.accessToken?.takeIf(String::isNotBlank)
                ?: error("Empty token in profile switch response")
            sessionEstablisher.establish(scope.serverId, api, accessToken, expectedAccountId = profileId)
        }.onFailure { cookieJar.cancelAuthentication(scope.serverId) }
    }

    private suspend fun <T> call(block: suspend (TypeTypeApi) -> T): Result<T> = suspendRunCatching {
        val scope = activeAccountScope.require()
        val value = withContext(Dispatchers.IO) { block(apiHolder.require(scope)) }
        activeAccountScope.verify(scope)
        value
    }
}

private fun AccountProfileDto.toDomain() = AccountProfile(
    id = id,
    name = name,
    isActive = isActive,
    isDefault = isDefault,
    avatarUrl = avatarUrl?.takeIf(String::isNotBlank),
)
