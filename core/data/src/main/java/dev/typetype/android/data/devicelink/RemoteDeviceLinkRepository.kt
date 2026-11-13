package dev.typetype.android.data.devicelink

import dev.typetype.android.core.error.suspendRunCatching
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApi
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.DeviceUserCodeRequest
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.devicelink.DeviceLinkRepository
import dev.typetype.android.domain.devicelink.DeviceLinkRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class RemoteDeviceLinkRepository @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
) : DeviceLinkRepository {
    override suspend fun lookup(userCode: String): Result<DeviceLinkRequest> = call { api ->
        val response = api.lookupDeviceLink(normalized(userCode))
        response.requireSuccessfulResponse()
        val body = response.body() ?: error("The instance returned an empty device request")
        DeviceLinkRequest(userCode = body.userCode, deviceName = body.deviceName, expiresAtMillis = body.expiresAt)
    }

    override suspend fun approve(userCode: String): Result<Unit> = call { api ->
        api.approveDeviceLink(DeviceUserCodeRequest(normalized(userCode))).requireSuccessfulResponse()
    }

    override suspend fun deny(userCode: String): Result<Unit> = call { api ->
        api.denyDeviceLink(DeviceUserCodeRequest(normalized(userCode))).requireSuccessfulResponse()
    }

    private fun normalized(userCode: String) = userCode.trim().uppercase().filter { it.isLetterOrDigit() || it == '-' }

    private suspend fun <T> call(block: suspend (TypeTypeApi) -> T): Result<T> = suspendRunCatching {
        val scope = activeAccountScope.require()
        val value = withContext(Dispatchers.IO) { block(apiHolder.require(scope)) }
        activeAccountScope.verify(scope)
        value
    }
}
