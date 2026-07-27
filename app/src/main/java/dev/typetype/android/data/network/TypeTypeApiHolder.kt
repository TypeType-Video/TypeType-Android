package dev.typetype.android.data.network

import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.domain.server.ServerRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TypeTypeApiHolder @Inject constructor(
    private val scopedApiFactory: ScopedApiFactory,
    private val serverRepository: ServerRepository,
    private val tokenStore: AccessTokenStore,
) {
    suspend fun require(scope: AccountScope): TypeTypeApi = create(scope) { baseUrl, token ->
        scopedApiFactory.create(
            baseUrl = baseUrl,
            serverId = scope.serverId,
            accountId = scope.accountId,
            token = token,
            type = TypeTypeApi::class.java,
        )
    }

    suspend fun requireSabr(scope: AccountScope): TypeTypeMediaApi = create(scope) { baseUrl, token ->
        scopedApiFactory.createSabr(
            baseUrl = baseUrl,
            serverId = scope.serverId,
            accountId = scope.accountId,
            token = token,
            type = TypeTypeMediaApi::class.java,
        )
    }

    suspend fun requireSupport(scope: AccountScope): TypeTypeSupportApi = create(scope) { baseUrl, token ->
        scopedApiFactory.create(
            baseUrl = baseUrl,
            serverId = scope.serverId,
            accountId = scope.accountId,
            token = token,
            type = TypeTypeSupportApi::class.java,
        )
    }

    suspend fun requireActiveSession(scope: AccountScope): TypeTypeActiveSessionApi =
        create(scope) { baseUrl, token ->
            scopedApiFactory.create(
                baseUrl = baseUrl,
                serverId = scope.serverId,
                accountId = scope.accountId,
                token = token,
                type = TypeTypeActiveSessionApi::class.java,
            )
        }

    private suspend fun <T> create(
        scope: AccountScope,
        factory: (baseUrl: String, token: String) -> T,
    ): T {
        val server = serverRepository.getServer(scope.serverId)
            ?: error("Instance not found")
        val token = tokenStore.getAccessToken(scope.serverId, scope.accountId)
            ?: error("This account needs to sign in again")
        return factory(server.baseUrl, token)
    }
}
