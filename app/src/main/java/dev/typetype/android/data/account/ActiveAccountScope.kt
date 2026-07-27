package dev.typetype.android.data.account

import dev.typetype.android.domain.server.ServerRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class AccountScope(
    val serverId: String,
    val accountId: String,
)

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class ActiveAccountScope @Inject constructor(
    serverRepository: ServerRepository,
    accountScopeStore: AccountScopeStore,
) {
    private val activeScope = serverRepository.observeCurrentServer()
        .flatMapLatest { server ->
            if (server == null) {
                flowOf(null)
            } else {
                accountScopeStore.observeCurrentAccountId(server.id).map { accountId ->
                    accountId?.let { AccountScope(server.id, it) }
                }
            }
        }
        .distinctUntilChanged()

    fun observe(): Flow<AccountScope?> = activeScope

    suspend fun require(): AccountScope =
        requireNotNull(activeScope.first()) { "No account is currently selected" }

    suspend fun verify(expected: AccountScope) {
        check(require() == expected) { "The active account changed during the request" }
    }
}

data class AccountScopedValue<T>(
    val scope: AccountScope,
    val value: T,
)
