package dev.typetype.android.data.usersettings

import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.account.AccountScopedValue
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.domain.usersettings.UserSettings
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class RemoteUserSettingsRepository @Inject constructor(
    private val network: UserSettingsNetworkSource,
    private val activeAccountScope: ActiveAccountScope,
) : UserSettingsRepository {

    private val state = MutableStateFlow<AccountScopedValue<UserSettings>?>(null)
    private val mutex = Mutex()

    override fun observe(): Flow<UserSettings> =
        combine(activeAccountScope.observe(), state) { scope, cached ->
            cached?.value?.takeIf { scope == cached.scope } ?: UserSettings()
        }

    override suspend fun current(): Result<UserSettings> = captureResult {
        mutex.withLock {
            val scope = activeAccountScope.require()
            state.value?.takeIf { it.scope == scope }?.value ?: refreshLocked(scope)
        }
    }

    override suspend fun refresh(): Result<Unit> = captureResult {
        mutex.withLock {
            refreshLocked(activeAccountScope.require())
        }
    }

    override suspend fun update(transform: (UserSettings) -> UserSettings): Result<Unit> =
        captureResult {
            mutex.withLock {
                val scope = activeAccountScope.require()
                val fresh = refreshLocked(scope)
                val next = transform(fresh)
                val patch = next.patchFrom(fresh)
                if (patch.isNotEmpty()) {
                    val updated = network.update(scope, patch)
                    activeAccountScope.verify(scope)
                    state.value = AccountScopedValue(scope, updated)
                }
            }
        }

    private suspend fun refreshLocked(scope: AccountScope): UserSettings {
        val fresh = network.fetch(scope)
        activeAccountScope.verify(scope)
        state.value = AccountScopedValue(scope, fresh)
        return fresh
    }
}

private suspend fun <T> captureResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Throwable) {
    Result.failure(failure)
}
