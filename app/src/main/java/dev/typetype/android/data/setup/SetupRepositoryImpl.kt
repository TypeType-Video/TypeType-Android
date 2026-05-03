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
        val normalized = normalizeUrl(rawUrl)
        val api = retrofitFactory.create(normalized)
        val response = withContext(Dispatchers.IO) { api.guest() }
        if (!response.isSuccessful) {
            error("Server responded with HTTP ${response.code()}")
        }
        ProbeResult(
            normalizedUrl = normalized,
            derivedDisplayName = displayNameFrom(normalized),
        )
    }

    override suspend fun persistServer(server: Server, makeCurrent: Boolean) {
        serverRepository.addServer(server)
        if (makeCurrent) serverRepository.setCurrentServer(server.id)
    }

    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        require(trimmed.isNotEmpty()) { "URL cannot be empty" }
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
        return withScheme
    }

    private fun displayNameFrom(url: String): String =
        url.removePrefix("https://").removePrefix("http://").substringBefore('/')
}
