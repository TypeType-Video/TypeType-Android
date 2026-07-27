package dev.typetype.android.data.auth

import dev.typetype.android.data.network.AccessTokenStore
import dev.typetype.android.data.account.AccountDao
import dev.typetype.android.data.account.AccountEntity
import dev.typetype.android.data.account.AccountScopeStore
import dev.typetype.android.data.network.PersistentCookieJar
import dev.typetype.android.data.network.RetrofitFactory
import dev.typetype.android.data.network.ScopedApiFactory
import dev.typetype.android.data.network.TypeTypeApi
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.data.network.dto.LoginRequest
import dev.typetype.android.data.network.dto.OidcCallbackRequest
import dev.typetype.android.domain.auth.AuthRepository
import dev.typetype.android.domain.auth.OidcAuthorization
import dev.typetype.android.domain.auth.OidcCallbackParser
import dev.typetype.android.domain.auth.SessionStatus
import dev.typetype.android.domain.server.ServerRepository
import dev.typetype.android.domain.setup.ServerAddress
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val serverRepository: ServerRepository,
    private val retrofitFactory: RetrofitFactory,
    private val tokenStore: AccessTokenStore,
    private val cookieJar: PersistentCookieJar,
    private val accountDao: AccountDao,
    private val accountScopeStore: AccountScopeStore,
    private val oidcTransactionStore: OidcTransactionStore,
    private val scopedApiFactory: ScopedApiFactory,
) : AuthRepository {

    override suspend fun loginWithCredentials(
        serverId: String,
        identifier: String,
        password: String,
    ): Result<Unit> = runCatching {
        val server = requireNotNull(serverRepository.getServer(serverId)) { "Server not found" }
        cookieJar.beginAuthentication(serverId, server.baseUrl)
        val api = retrofitFactory.createWithoutAutomaticAuthentication(server.baseUrl)
        val response = withContext(Dispatchers.IO) {
            api.login(LoginRequest(identifier = identifier, password = password))
        }
        response.requireSuccessfulResponse()
        val token = response.body()?.accessToken ?: error("Empty token in response")
        establishSession(serverId, api, token)
    }.onFailure { cookieJar.cancelAuthentication(serverId) }

    override suspend fun loginAsGuest(serverId: String): Result<Unit> = runCatching {
        val server = requireNotNull(serverRepository.getServer(serverId)) { "Server not found" }
        cookieJar.beginAuthentication(serverId, server.baseUrl)
        val api = retrofitFactory.createWithoutAutomaticAuthentication(server.baseUrl)
        val response = withContext(Dispatchers.IO) { api.guest() }
        response.requireSuccessfulResponse()
        val token = response.body()?.token ?: error("Empty token in response")
        establishSession(serverId, api, token)
    }.onFailure { cookieJar.cancelAuthentication(serverId) }

    override suspend fun validateSession(): SessionStatus {
        val server = serverRepository.observeCurrentServer().first() ?: return SessionStatus.Invalid
        val accountId = accountScopeStore.getCurrentAccountId(server.id) ?: return SessionStatus.Invalid
        val token = tokenStore.getAccessToken(server.id, accountId)
        if (token.isNullOrBlank()) return SessionStatus.Invalid
        val api = scopedApiFactory.create(
            baseUrl = server.baseUrl,
            serverId = server.id,
            accountId = accountId,
            token = token,
            type = TypeTypeApi::class.java,
        )
        return try {
            val response = withContext(Dispatchers.IO) { api.me() }
            when {
                response.isSuccessful && response.body() != null -> {
                    val profile = requireNotNull(response.body())
                    check(profile.id == accountId) { "The session belongs to another account" }
                    val activeToken = tokenStore.getAccessToken(server.id, accountId)
                        ?: return SessionStatus.Invalid
                    val generation = accountDao.get(server.id, profile.id)?.sessionGeneration
                        ?: System.currentTimeMillis()
                    accountDao.upsert(AccountEntity.fromProfile(server.id, profile, generation))
                    tokenStore.setAuthenticatedAccessToken(server.id, profile.id, activeToken)
                    SessionStatus.Valid
                }
                response.code() == 401 || response.code() == 403 -> SessionStatus.Invalid
                else -> SessionStatus.Unknown
            }
        } catch (_: IOException) {
            SessionStatus.Unknown
        } catch (_: Exception) {
            SessionStatus.Unknown
        }
    }

    override suspend fun logout(serverId: String): Result<Unit> {
        val server = serverRepository.getServer(serverId)
            ?: return Result.failure(IllegalStateException("Server not found"))
        val remoteResult = runCatching {
            val token = tokenStore.getAccessToken(serverId)
            val api = retrofitFactory.createWithoutAutomaticAuthentication(server.baseUrl)
            val response = withContext(Dispatchers.IO) {
                api.logout(token?.let { "Bearer $it" })
            }
            response.requireSuccessfulResponse()
        }
        tokenStore.setAccessToken(serverId, null)
        server.baseUrl.toHttpUrlOrNull()?.let { cookieJar.clearCurrentSession(serverId, it.host) }
        val failure = remoteResult.exceptionOrNull()
        return if (failure == null) Result.success(Unit) else Result.failure(failure)
    }

    override suspend fun startOidc(serverId: String): Result<OidcAuthorization> = runCatching {
        val server = requireNotNull(serverRepository.getServer(serverId)) { "Instance not found" }
        require(server.oidcEnabled) { "OIDC is not enabled on this instance" }
        cookieJar.beginAuthentication(serverId, server.baseUrl)
        val api = retrofitFactory.createWithoutAutomaticAuthentication(server.baseUrl)
        val response = withContext(Dispatchers.IO) { api.startOidc(OIDC_REDIRECT_URI) }
        response.requireSuccessfulResponse()
        val authorizationUrl = response.body()?.authorizationUrl
            ?: error("The instance returned an empty authorization URL")
        val parsedUrl = authorizationUrl.toHttpUrlOrNull()
            ?: error("The instance returned an invalid authorization URL")
        require(parsedUrl.isHttps || ServerAddress.requiresLocalNetworkAccess(authorizationUrl)) {
            "OIDC authorization must use HTTPS outside the local network"
        }
        val state = parsedUrl.queryParameterValues("state").singleOrNull()
            ?: error("The OIDC authorization URL is missing a unique state")
        oidcTransactionStore.start(serverId, state)
        OidcAuthorization(
            authorizationUrl = authorizationUrl,
            redirectScheme = OIDC_REDIRECT_SCHEME,
        )
    }.onFailure {
        oidcTransactionStore.clear(serverId)
        cookieJar.cancelAuthentication(serverId)
    }

    override suspend fun finishOidc(serverId: String, callbackUrl: String): Result<Unit> = runCatching {
        val callback = OidcCallbackParser.parse(callbackUrl, OIDC_REDIRECT_SCHEME)
        oidcTransactionStore.requireMatches(serverId, callback.state)
        val server = requireNotNull(serverRepository.getServer(serverId)) { "Instance not found" }
        cookieJar.resumeAuthentication(serverId, server.baseUrl)
        val response = withContext(Dispatchers.IO) {
            retrofitFactory.createWithoutAutomaticAuthentication(server.baseUrl).finishOidc(
                OidcCallbackRequest(
                    code = callback.code,
                    state = callback.state,
                    redirectUri = OIDC_REDIRECT_URI,
                ),
            )
        }
        response.requireSuccessfulResponse()
        val token = response.body()?.accessToken ?: error("Empty token in OIDC response")
        establishSession(
            serverId,
            retrofitFactory.createWithoutAutomaticAuthentication(server.baseUrl),
            token,
        )
    }.onFailure { cookieJar.cancelAuthentication(serverId) }
        .also { oidcTransactionStore.clear(serverId) }

    override suspend fun cancelOidc(serverId: String) {
        oidcTransactionStore.clear(serverId)
        cookieJar.cancelAuthentication(serverId)
    }

    private suspend fun establishSession(serverId: String, api: dev.typetype.android.data.network.TypeTypeApi, token: String) {
        val profileResponse = withContext(Dispatchers.IO) { api.me("Bearer $token") }
        profileResponse.requireSuccessfulResponse()
        val profile = profileResponse.body() ?: error("The instance returned an empty account")
        val previousGeneration = accountDao.get(serverId, profile.id)?.sessionGeneration ?: 0L
        val generation = maxOf(System.currentTimeMillis(), previousGeneration + 1L)
        accountDao.upsert(AccountEntity.fromProfile(serverId, profile, generation))
        tokenStore.setAuthenticatedAccessToken(serverId, profile.id, token)
        cookieJar.completeAuthentication(serverId, profile.id)
    }

    private companion object {
        const val OIDC_REDIRECT_SCHEME = "dev.typetype.android"
        const val OIDC_REDIRECT_URI = "$OIDC_REDIRECT_SCHEME://oidc/callback"
    }
}
