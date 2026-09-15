package dev.typetype.android.data.push

import dev.typetype.android.data.account.AccountDao
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.ChannelNotificationPreferenceRequestDto
import dev.typetype.android.data.network.dto.PushDeviceRegistrationRequestDto
import dev.typetype.android.data.network.dto.toDomain
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.push.ChannelNotificationsPreference
import dev.typetype.android.domain.push.PushDevice
import dev.typetype.android.domain.push.PushRepository
import dev.typetype.android.domain.server.PushCapability
import dev.typetype.android.domain.server.ServerCapabilitiesRepository
import dev.typetype.android.domain.server.ServerRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class RemotePushRepository @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
    private val accountDao: AccountDao,
    private val capabilitiesRepository: ServerCapabilitiesRepository,
    private val serverRepository: ServerRepository,
) : PushRepository {

    override suspend fun currentCapability(): PushCapability {
        val scope = activeAccountScope.require()
        val refreshed = runCatching { capabilitiesRepository.refresh(scope.serverId).getOrThrow() }
        return refreshed.getOrNull()?.push
            ?: serverRepository.getServer(scope.serverId)?.push
            ?: PushCapability()
    }

    override suspend fun registerDevice(deviceId: String, endpoint: String): Result<Unit> = guarded {
        val api = apiHolder.require(requireEligibleScope())
        val response = withContext(Dispatchers.IO) {
            api.registerPushDevice(
                PushDeviceRegistrationRequestDto(deviceId = deviceId, endpoint = endpoint),
            )
        }
        response.requireSuccessfulResponse()
    }

    override suspend fun unregisterDevice(deviceId: String): Result<Unit> = guarded {
        val api = apiHolder.require(requireEligibleScope())
        val response = withContext(Dispatchers.IO) { api.deletePushDevice(deviceId) }
        if (response.code() != 404) {
            response.requireSuccessfulResponse()
        }
    }

    override suspend fun devices(): Result<List<PushDevice>> = guarded {
        val api = apiHolder.require(requireEligibleScope())
        val response = withContext(Dispatchers.IO) { api.pushDevices() }
        response.requireSuccessfulResponse()
        response.body()?.map { it.toDomain() }.orEmpty()
    }

    override suspend fun channelPreferences(): Result<List<ChannelNotificationsPreference>> = guarded {
        val api = apiHolder.require(requireEligibleScope())
        val response = withContext(Dispatchers.IO) { api.channelNotificationPreferences() }
        response.requireSuccessfulResponse()
        response.body()?.map { it.toDomain() }.orEmpty()
    }

    override suspend fun setChannelPreference(channelUrl: String, enabled: Boolean): Result<Unit> = guarded {
        val api = apiHolder.require(requireEligibleScope())
        val response = withContext(Dispatchers.IO) {
            api.setChannelNotificationPreference(
                ChannelNotificationPreferenceRequestDto(channelUrl = channelUrl, enabled = enabled),
            )
        }
        response.requireSuccessfulResponse()
    }

    private suspend fun requireEligibleScope(): AccountScope {
        val scope = activeAccountScope.require()
        val account = accountDao.get(scope.serverId, scope.accountId)
        check(account != null && !account.isGuest) {
            "Push notifications are unavailable for guest accounts"
        }
        return scope
    }

    private inline fun <T> guarded(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }
}
