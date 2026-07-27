package dev.typetype.android.data.session

import dev.typetype.android.BuildConfig
import dev.typetype.android.data.account.AccountDao
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeActiveSessionApi
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.SessionDeviceRequest
import dev.typetype.android.data.network.dto.SessionPlaybackRequest
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.session.ActivePlaybackSnapshot
import dev.typetype.android.domain.session.ActiveSessionRepository
import dev.typetype.android.domain.stream.StreamRequestScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow

@Singleton
class RemoteActiveSessionRepository @Inject constructor(
    private val activeAccountScope: ActiveAccountScope,
    private val accountDao: AccountDao,
    private val apiHolder: TypeTypeApiHolder,
    private val deviceIdentityStore: DeviceIdentityStore,
) : ActiveSessionRepository {

    override fun observeDeviceName(): Flow<String> = deviceIdentityStore.observeDeviceName()

    override suspend fun setDeviceName(name: String) {
        withContext(Dispatchers.IO) { deviceIdentityStore.setDeviceName(name) }
    }

    override suspend fun reportActivity(): Result<Unit> = runCatching {
        val scope = activeAccountScope.require()
        if (!scope.canReport()) return@runCatching
        val response = withContext(Dispatchers.IO) {
            apiHolder.requireActiveSession(scope).reportActivity(deviceRequest())
        }
        response.requireSuccessfulResponse()
        activeAccountScope.verify(scope)
    }

    override suspend fun reportPlaybackStart(
        requestScope: StreamRequestScope,
        snapshot: ActivePlaybackSnapshot,
    ): Result<Unit> = reportPlayback(requestScope, snapshot) { api, request ->
        api.reportPlaybackStart(request)
    }

    override suspend fun reportPlaybackProgress(
        requestScope: StreamRequestScope,
        snapshot: ActivePlaybackSnapshot,
    ): Result<Unit> = reportPlayback(requestScope, snapshot) { api, request ->
        api.reportPlaybackProgress(request)
    }

    override suspend fun reportPlaybackStop(requestScope: StreamRequestScope): Result<Unit> = runCatching {
        val scope = requestScope.accountScope()
        activeAccountScope.verify(scope)
        if (!scope.canReport()) return@runCatching
        val response = withContext(Dispatchers.IO) {
            apiHolder.requireActiveSession(scope).reportPlaybackStop(deviceRequest())
        }
        response.requireSuccessfulResponse()
        activeAccountScope.verify(scope)
    }

    private suspend fun reportPlayback(
        requestScope: StreamRequestScope,
        snapshot: ActivePlaybackSnapshot,
        request: suspend (TypeTypeActiveSessionApi, SessionPlaybackRequest) -> retrofit2.Response<Unit>,
    ): Result<Unit> = runCatching {
        val scope = requestScope.accountScope()
        activeAccountScope.verify(scope)
        if (!scope.canReport()) return@runCatching
        val response = withContext(Dispatchers.IO) {
            request(apiHolder.requireActiveSession(scope), snapshot.toRequest())
        }
        response.requireSuccessfulResponse()
        activeAccountScope.verify(scope)
    }

    private suspend fun AccountScope.canReport(): Boolean =
        accountDao.get(serverId, accountId)?.isGuest == false

    private fun StreamRequestScope.accountScope(): AccountScope = AccountScope(serverId, accountId)

    private fun deviceRequest(): SessionDeviceRequest = SessionDeviceRequest(
        clientName = CLIENT_NAME,
        clientVersion = BuildConfig.VERSION_NAME,
        deviceId = deviceIdentityStore.getOrCreate(),
        deviceName = deviceIdentityStore.getDeviceName().trim().ifBlank { DEFAULT_DEVICE_NAME },
        deviceType = DEVICE_TYPE,
    )

    private fun ActivePlaybackSnapshot.toRequest(): SessionPlaybackRequest {
        val device = deviceRequest()
        return SessionPlaybackRequest(
            clientName = device.clientName,
            clientVersion = device.clientVersion,
            deviceId = device.deviceId,
            deviceName = device.deviceName,
            deviceType = device.deviceType,
            videoUrl = videoUrl,
            title = title,
            thumbnail = thumbnailUrl,
            channelName = channelName,
            positionMs = positionMillis.coerceAtLeast(0L),
            durationMs = durationMillis?.takeIf { it > 0L },
            paused = isPaused,
        )
    }

    private companion object {
        const val CLIENT_NAME = "TypeType Android"
        const val DEFAULT_DEVICE_NAME = "Android device"
        const val DEVICE_TYPE = "android_phone"
    }
}
