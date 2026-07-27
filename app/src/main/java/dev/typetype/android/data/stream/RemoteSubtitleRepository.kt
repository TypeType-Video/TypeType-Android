package dev.typetype.android.data.stream

import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.serverResponseException
import dev.typetype.android.domain.stream.StreamSubtitleSource
import dev.typetype.android.domain.stream.SubtitleRepository
import dev.typetype.android.domain.server.ServerRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class RemoteSubtitleRepository @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val activeAccountScope: ActiveAccountScope,
    private val serverRepository: ServerRepository,
) : SubtitleRepository {
    override suspend fun load(source: StreamSubtitleSource): Result<ByteArray> = try {
        val scope = activeAccountScope.require()
        val server = serverRepository.getServer(scope.serverId) ?: error("Instance not found")
        val url = resolveServerUrl(server.baseUrl, source.url)
            ?: error("Subtitle track left its TypeType instance")
        val api = apiHolder.requireSabr(scope)
        val response = withContext(Dispatchers.IO) { api.subtitle(url) }
        if (!response.isSuccessful) throw serverResponseException(response)
        check(resolveServerUrl(server.baseUrl, response.raw().request.url.toString()) != null) {
            "Subtitle response left its TypeType instance"
        }
        val length = response.body()?.contentLength() ?: -1L
        require(length <= MAX_SUBTITLE_BYTES) { "Subtitle track is too large" }
        val bytes = withContext(Dispatchers.IO) {
            response.body()?.bytes() ?: error("Empty subtitle response")
        }
        require(bytes.size <= MAX_SUBTITLE_BYTES) { "Subtitle track is too large" }
        activeAccountScope.verify(scope)
        Result.success(bytes)
    } catch (failure: CancellationException) {
        throw failure
    } catch (failure: Throwable) {
        Result.failure(failure)
    }

    private companion object {
        const val MAX_SUBTITLE_BYTES = 5 * 1024 * 1024
    }
}
