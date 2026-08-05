package dev.typetype.android.data.youtubesession

import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.YoutubeRemoteBrowserStartRequest
import dev.typetype.android.data.network.dto.YoutubeRemoteBrowserStartResponse
import dev.typetype.android.data.network.dto.YoutubeSessionStatusResponse
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.youtubesession.YoutubeRemoteBrowserSession
import dev.typetype.android.domain.youtubesession.YoutubeSession
import dev.typetype.android.domain.youtubesession.YoutubeSessionRepository
import dev.typetype.android.domain.youtubesession.YoutubeSessionStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class RemoteYoutubeSessionRepository @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
) : YoutubeSessionRepository {
    override suspend fun getStatus(): Result<YoutubeSession> = runCatching {
        val scope = activeAccountScope.require()
        val response = withContext(Dispatchers.IO) {
            apiHolder.require(scope).youtubeSessionStatus()
        }
        response.requireSuccessfulResponse()
        val status = response.body() ?: error("The instance returned an empty YouTube session status")
        activeAccountScope.verify(scope)
        status.toDomain()
    }

    override suspend fun startRemoteBrowser(
        returnTo: String?,
    ): Result<YoutubeRemoteBrowserSession> = runCatching {
        val scope = activeAccountScope.require()
        val response = withContext(Dispatchers.IO) {
            apiHolder.require(scope).startYoutubeRemoteBrowser(
                YoutubeRemoteBrowserStartRequest(returnTo = returnTo),
            )
        }
        response.requireSuccessfulResponse()
        val session = response.body() ?: error("The instance returned an empty remote browser session")
        activeAccountScope.verify(scope)
        session.toDomain()
    }

    override suspend fun cancelRemoteBrowser(sessionId: String): Result<Unit> = runCatching {
        val scope = activeAccountScope.require()
        val response = withContext(Dispatchers.IO) {
            apiHolder.require(scope).cancelYoutubeRemoteBrowser(sessionId)
        }
        response.requireSuccessfulResponse()
        activeAccountScope.verify(scope)
    }

    override suspend fun disconnect(): Result<Unit> = runCatching {
        val scope = activeAccountScope.require()
        val response = withContext(Dispatchers.IO) {
            apiHolder.require(scope).disconnectYoutubeSession()
        }
        response.requireSuccessfulResponse()
        activeAccountScope.verify(scope)
    }
}

internal fun YoutubeSessionStatusResponse.toDomain(): YoutubeSession = YoutubeSession(
    status = when (status) {
        "disconnected" -> YoutubeSessionStatus.Disconnected
        "connected" -> YoutubeSessionStatus.Connected
        "needs_reconnect" -> YoutubeSessionStatus.NeedsReconnect
        else -> YoutubeSessionStatus.Unknown
    },
    updatedAt = updatedAt,
    lastUsedAt = lastUsedAt,
)

private fun YoutubeRemoteBrowserStartResponse.toDomain() = YoutubeRemoteBrowserSession(
    sessionId = sessionId,
    webSocketUrl = wsUrl,
    expiresAt = expiresAt,
)
