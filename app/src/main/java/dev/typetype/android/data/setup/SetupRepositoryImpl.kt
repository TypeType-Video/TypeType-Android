package dev.typetype.android.data.setup

import dev.typetype.android.data.network.RetrofitFactory
import dev.typetype.android.domain.server.Server
import dev.typetype.android.domain.server.ServerRepository
import dev.typetype.android.domain.setup.ProbeResult
import dev.typetype.android.domain.setup.ServerAddress
import dev.typetype.android.domain.setup.SetupRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class SetupRepositoryImpl @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
    private val serverRepository: ServerRepository,
) : SetupRepository {

    override suspend fun probeServer(rawUrl: String): Result<ProbeResult> = runCatching {
        val candidates = ServerAddress.candidateBaseUrls(rawUrl)
        val attempts = candidates.map { tryCandidate(it) }
        val resolved = attempts.filterIsInstance<Attempt.Resolved>().firstOrNull()?.value
            ?: throw probeFailure(attempts)
        require(resolved.instance.apiVersion == SUPPORTED_API_VERSION) {
            "This instance uses API ${resolved.instance.apiVersion}, but this app supports API $SUPPORTED_API_VERSION"
        }
        ProbeResult(
            normalizedUrl = resolved.baseUrl,
            name = resolved.instance.name,
            tagline = resolved.instance.tagline,
            version = resolved.instance.version,
            revision = resolved.instance.revision,
            apiVersion = resolved.instance.apiVersion,
            registrationAllowed = resolved.instance.registrationAllowed,
            guestAllowed = resolved.instance.guestAllowed,
            supportedServices = resolved.instance.supportedServices,
            minAndroidClientVersion = resolved.instance.minClientVersion?.android,
            logoUrl = resolved.instance.logoUrl,
            bannerUrl = resolved.instance.bannerUrl,
            localLoginEnabled = resolved.instance.localLoginEnabled,
            oidcEnabled = resolved.instance.oidcEnabled,
            oidcProviderName = resolved.instance.oidcProviderName,
            oidcAutoRedirect = resolved.instance.oidcAutoRedirect,
            youtubeRemoteLoginEnabled = resolved.instance.youtubeRemoteLoginEnabled,
            youtubeRemoteLoginReady = resolved.instance.youtubeRemoteLoginReady,
            youtubeRemoteLoginUnavailableReason = resolved.instance.youtubeRemoteLoginUnavailableReason,
        )
    }

    override suspend fun persistServer(server: Server, makeCurrent: Boolean) {
        serverRepository.addServer(server)
        if (makeCurrent) serverRepository.setCurrentServer(server.id)
    }

    private suspend fun tryCandidate(baseUrl: String): Attempt {
        val api = retrofitFactory.create(baseUrl)
        return try {
            val healthResponse = withContext(Dispatchers.IO) { api.health() }
            if (!healthResponse.isSuccessful || healthResponse.body()?.status != "ok") {
                return Attempt.UnexpectedResponse
            }
            val instanceResponse = withContext(Dispatchers.IO) { api.instance() }
            val body = instanceResponse.body()
            if (instanceResponse.isSuccessful && body != null) {
                Attempt.Resolved(Resolved(baseUrl, body))
            } else {
                Attempt.UnexpectedResponse
            }
        } catch (error: java.io.IOException) {
            Attempt.NetworkFailure(error)
        } catch (error: Exception) {
            Attempt.UnexpectedPayload(error)
        }
    }

    private fun probeFailure(attempts: List<Attempt>): Exception {
        val payloadFailure = attempts.filterIsInstance<Attempt.UnexpectedPayload>().firstOrNull()
        return when {
            payloadFailure != null || attempts.any { it == Attempt.UnexpectedResponse } ->
                IllegalStateException("The address responded, but it is not a compatible TypeType instance")
            else -> {
                val cause = attempts.filterIsInstance<Attempt.NetworkFailure>().lastOrNull()?.cause
                java.io.IOException("Could not reach a TypeType instance at this address", cause)
            }
        }
    }

    private data class Resolved(
        val baseUrl: String,
        val instance: dev.typetype.android.data.network.dto.InstanceResponse,
    )

    private sealed interface Attempt {
        data class Resolved(val value: SetupRepositoryImpl.Resolved) : Attempt
        data class NetworkFailure(val cause: java.io.IOException) : Attempt
        data class UnexpectedPayload(val cause: Exception) : Attempt
        data object UnexpectedResponse : Attempt
    }

    private companion object {
        const val SUPPORTED_API_VERSION = 1
    }
}
