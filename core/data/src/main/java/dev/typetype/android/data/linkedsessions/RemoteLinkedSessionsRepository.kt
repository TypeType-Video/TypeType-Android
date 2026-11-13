package dev.typetype.android.data.linkedsessions

import dev.typetype.android.core.error.suspendRunCatching
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApi
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.BiliBiliQrPollRequest
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.linkedsessions.BiliBiliQrLogin
import dev.typetype.android.domain.linkedsessions.BiliBiliQrPoll
import dev.typetype.android.domain.linkedsessions.BiliBiliSessionRepository
import dev.typetype.android.domain.linkedsessions.BiliBiliSessionState
import dev.typetype.android.domain.linkedsessions.YoutubeDeviceLogin
import dev.typetype.android.domain.linkedsessions.YoutubeDeviceLoginRepository
import dev.typetype.android.domain.linkedsessions.YoutubeDevicePoll
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

@Singleton
class RemoteLinkedSessionsRepository @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
) : YoutubeDeviceLoginRepository, BiliBiliSessionRepository {
    override suspend fun start(): Result<YoutubeDeviceLogin> = call { api ->
        val body = api.startYoutubeDeviceLogin().bodyOrFail()
        YoutubeDeviceLogin(
            userCode = body.userCode,
            verificationUrl = body.verificationUrl,
            expiresAtMillis = body.expiresAt,
            intervalSeconds = body.intervalSeconds.coerceIn(MIN_POLL_SECONDS, MAX_POLL_SECONDS),
        )
    }

    override suspend fun poll(): Result<YoutubeDevicePoll> = call { api ->
        when (api.pollYoutubeDeviceLogin().bodyOrFail().status) {
            "connected" -> YoutubeDevicePoll.Connected
            "slow_down" -> YoutubeDevicePoll.SlowDown
            else -> YoutubeDevicePoll.Pending
        }
    }

    override suspend fun cancel(): Result<Unit> = call { api -> api.cancelYoutubeDeviceLogin().requireSuccessfulResponse() }

    override suspend fun state(): Result<BiliBiliSessionState> = call { api ->
        when (api.biliBiliSessionStatus().bodyOrFail().status) {
            "connected" -> BiliBiliSessionState.Connected
            "needs_reconnect" -> BiliBiliSessionState.NeedsReconnect
            else -> BiliBiliSessionState.Disconnected
        }
    }

    override suspend fun startQrLogin(): Result<BiliBiliQrLogin> = call { api ->
        val body = api.startBiliBiliQrLogin().bodyOrFail()
        BiliBiliQrLogin(qrUrl = body.qrUrl, qrcodeKey = body.qrcodeKey, expiresAtMillis = body.expiresAt)
    }

    override suspend fun pollQrLogin(qrcodeKey: String): Result<BiliBiliQrPoll> = call { api ->
        when (api.pollBiliBiliQrLogin(BiliBiliQrPollRequest(qrcodeKey)).bodyOrFail().status) {
            "waiting" -> BiliBiliQrPoll.Waiting
            "scanned" -> BiliBiliQrPoll.Scanned
            "confirmed" -> BiliBiliQrPoll.Confirmed
            "expired" -> BiliBiliQrPoll.Expired
            else -> BiliBiliQrPoll.Failed
        }
    }

    override suspend fun disconnect(): Result<Unit> = call { api -> api.disconnectBiliBiliSession().requireSuccessfulResponse() }

    private suspend fun <T> call(block: suspend (TypeTypeApi) -> T): Result<T> = suspendRunCatching {
        val scope = activeAccountScope.require()
        val value = withContext(Dispatchers.IO) { block(apiHolder.require(scope)) }
        activeAccountScope.verify(scope)
        value
    }

    private companion object {
        const val MIN_POLL_SECONDS = 2
        const val MAX_POLL_SECONDS = 30
    }
}

private fun <T> Response<T>.bodyOrFail(): T {
    requireSuccessfulResponse()
    return body() ?: error("The instance returned an empty response")
}
