package dev.typetype.android.data.server

import dev.typetype.android.data.network.RetrofitFactory
import dev.typetype.android.data.network.dto.InstanceResponse
import dev.typetype.android.data.network.requireSuccessfulResponse
import dev.typetype.android.domain.server.RssCapability
import dev.typetype.android.domain.server.Server
import dev.typetype.android.domain.server.ServerCapabilitiesRepository
import dev.typetype.android.domain.server.ServerRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class RemoteServerCapabilitiesRepository @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
    private val serverRepository: ServerRepository,
) : ServerCapabilitiesRepository {
    override suspend fun refresh(serverId: String): Result<Server> = captureResult {
        val server = serverRepository.getServer(serverId) ?: error("Instance not found")
        val instance = withContext(Dispatchers.IO) {
            val response = retrofitFactory.create(server.baseUrl).instance()
            response.requireSuccessfulResponse()
            response.body() ?: error("Empty instance response")
        }
        val refreshed = server.withCapabilities(instance)
        serverRepository.addServer(refreshed)
        refreshed
    }
}

internal fun Server.withCapabilities(instance: InstanceResponse): Server = copy(
    displayName = instance.name,
    tagline = instance.tagline,
    version = instance.version,
    revision = instance.revision,
    apiVersion = instance.apiVersion,
    logoUrl = instance.logoUrl,
    bannerUrl = instance.bannerUrl,
    supportedServices = instance.supportedServices,
    minAndroidClientVersion = instance.minClientVersion?.android,
    registrationAllowed = instance.registrationAllowed,
    guestAllowed = instance.guestAllowed,
    localLoginEnabled = instance.localLoginEnabled,
    oidcEnabled = instance.oidcEnabled,
    oidcProviderName = instance.oidcProviderName,
    oidcAutoRedirect = instance.oidcAutoRedirect,
    youtubeRemoteLoginSupported = instance.youtubeRemoteLoginEnabled != null ||
        instance.youtubeRemoteLoginReady != null ||
        instance.youtubeRemoteLoginUnavailableReason != null,
    youtubeRemoteLoginEnabled = instance.youtubeRemoteLoginEnabled == true,
    youtubeRemoteLoginReady = instance.youtubeRemoteLoginReady == true,
    youtubeRemoteLoginUnavailableReason = instance.youtubeRemoteLoginUnavailableReason,
    rss = instance.rss?.let {
        RssCapability(
            enabled = it.enabled,
            maxFeedsPerUser = it.maxFeedsPerUser,
            maxItems = it.maxItems,
            minimumPollMinutes = it.minimumPollMinutes,
            rateLimitPerMinute = it.rateLimitPerMinute,
        )
    } ?: RssCapability(),
)

private suspend fun <T> captureResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Throwable) {
    Result.failure(failure)
}
