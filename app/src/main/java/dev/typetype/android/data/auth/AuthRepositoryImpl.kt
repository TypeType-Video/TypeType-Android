package dev.typetype.android.data.auth

import dev.typetype.android.data.network.AccessTokenStore
import dev.typetype.android.data.network.RetrofitFactory
import dev.typetype.android.data.network.dto.LoginRequest
import dev.typetype.android.domain.auth.AuthRepository
import dev.typetype.android.domain.auth.SessionStatus
import dev.typetype.android.domain.server.ServerRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val serverRepository: ServerRepository,
    private val retrofitFactory: RetrofitFactory,
    private val tokenStore: AccessTokenStore,
) : AuthRepository {

    override suspend fun loginWithCredentials(
        serverId: String,
        identifier: String,
        password: String,
    ): Result<Unit> = runCatching {
        val server = requireNotNull(serverRepository.getServer(serverId)) { "Server not found" }
        val api = retrofitFactory.create(server.baseUrl)
        val response = withContext(Dispatchers.IO) {
            api.login(LoginRequest(identifier = identifier, password = password))
        }
        if (!response.isSuccessful) {
            error("Login failed (HTTP ${response.code()})")
        }
        val token = response.body()?.accessToken ?: error("Empty token in response")
        tokenStore.setAccessToken(token)
    }

    override suspend fun loginAsGuest(serverId: String): Result<Unit> = runCatching {
        val server = requireNotNull(serverRepository.getServer(serverId)) { "Server not found" }
        val api = retrofitFactory.create(server.baseUrl)
        val response = withContext(Dispatchers.IO) { api.guest() }
        if (!response.isSuccessful) {
            error("Guest login failed (HTTP ${response.code()})")
        }
        val token = response.body()?.token ?: error("Empty token in response")
        tokenStore.setAccessToken(token)
    }

    override suspend fun validateSession(): SessionStatus {
        if (tokenStore.getAccessToken().isNullOrBlank()) return SessionStatus.Invalid
        val server = serverRepository.observeCurrentServer().first() ?: return SessionStatus.Invalid
        val api = retrofitFactory.create(server.baseUrl)
        return try {
            val response = withContext(Dispatchers.IO) { api.me() }
            when {
                response.isSuccessful -> SessionStatus.Valid
                response.code() == 401 || response.code() == 403 -> SessionStatus.Invalid
                else -> SessionStatus.Unknown
            }
        } catch (_: IOException) {
            SessionStatus.Unknown
        } catch (_: Exception) {
            SessionStatus.Unknown
        }
    }
}
