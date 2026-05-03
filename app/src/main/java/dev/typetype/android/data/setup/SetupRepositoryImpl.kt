package dev.typetype.android.data.setup

import dev.typetype.android.data.network.RetrofitFactory
import dev.typetype.android.domain.server.Server
import dev.typetype.android.domain.server.ServerRepository
import dev.typetype.android.domain.setup.ProbeResult
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
        val candidates = candidateBaseUrls(rawUrl)
        val resolved = candidates.firstNotNullOfOrNull { tryCandidate(it) }
            ?: error("No TypeType API found at this address")
        ProbeResult(
            normalizedUrl = resolved.baseUrl,
            name = resolved.instance.name,
            tagline = resolved.instance.tagline,
            version = resolved.instance.version,
            apiVersion = resolved.instance.apiVersion,
            registrationAllowed = resolved.instance.registrationAllowed,
            guestAllowed = resolved.instance.guestAllowed,
            supportedServices = resolved.instance.supportedServices,
            minAndroidClientVersion = resolved.instance.minClientVersion?.android,
        )
    }

    override suspend fun persistServer(server: Server, makeCurrent: Boolean) {
        serverRepository.addServer(server)
        if (makeCurrent) serverRepository.setCurrentServer(server.id)
    }

    private suspend fun tryCandidate(baseUrl: String): Resolved? {
        val api = retrofitFactory.create(baseUrl)
        return runCatching {
            val instanceResponse = withContext(Dispatchers.IO) { api.instance() }
            val body = instanceResponse.body()
            if (instanceResponse.isSuccessful && body != null) {
                Resolved(baseUrl, body)
            } else {
                null
            }
        }.getOrNull()
    }

    private fun candidateBaseUrls(raw: String): List<String> {
        val trimmed = raw.trim().trimEnd('/')
        require(trimmed.isNotEmpty()) { "URL cannot be empty" }
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
        return if (withScheme.contains("/api")) {
            listOf(withScheme)
        } else {
            listOf(withScheme, "$withScheme/api")
        }
    }

    private data class Resolved(
        val baseUrl: String,
        val instance: dev.typetype.android.data.network.dto.InstanceResponse,
    )
}
