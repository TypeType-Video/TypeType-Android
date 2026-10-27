package dev.typetype.android.data.stream

import dev.typetype.android.data.network.AccessTokenStore
import dev.typetype.android.data.network.ScopedApiFactory
import dev.typetype.android.data.network.TypeTypeMediaApi
import dev.typetype.android.data.network.dto.AudioOnlyStreamResponse
import dev.typetype.android.data.network.serverResponseException
import dev.typetype.android.domain.stream.AudioOnlyStream
import dev.typetype.android.domain.stream.AudioOnlyStreamKind
import dev.typetype.android.domain.stream.AudioOnlyStreamRepository
import dev.typetype.android.domain.stream.StreamRequestScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class RemoteAudioOnlyStreamRepository @Inject constructor(
    private val scopedApiFactory: ScopedApiFactory,
    private val tokenStore: AccessTokenStore,
) : AudioOnlyStreamRepository {
    override suspend fun resolve(
        requestScope: StreamRequestScope,
        videoUrl: String,
        preferOriginal: Boolean,
        preferredLocale: String,
    ): Result<AudioOnlyStream> = cancellableStreamResult {
        val token = tokenStore.getAccessToken(requestScope.serverId, requestScope.accountId)
            ?: error("This account needs to sign in again")
        val api = scopedApiFactory.create(
            baseUrl = requestScope.baseUrl,
            serverId = requestScope.serverId,
            accountId = requestScope.accountId,
            token = token,
            type = TypeTypeMediaApi::class.java,
        )
        val response = withContext(Dispatchers.IO) {
            api.audioOnlyStream(videoUrl, preferOriginal, preferredLocale)
        }
        if (!response.isSuccessful) throw serverResponseException(response)
        response.body()?.toDomain(requestScope.baseUrl)
            ?: error("Empty audio-only stream body")
    }
}

internal fun AudioOnlyStreamResponse.toDomain(baseUrl: String): AudioOnlyStream {
    val kind = when (kind.lowercase()) {
        "progressive" -> AudioOnlyStreamKind.Progressive
        "hls" -> AudioOnlyStreamKind.Hls
        "dash" -> AudioOnlyStreamKind.Dash
        else -> error("Unsupported audio-only stream kind")
    }
    val sourceUrl = resolveServerUrl(baseUrl, src)
        ?: error("Invalid audio-only stream URL")
    return AudioOnlyStream(
        url = sourceUrl,
        kind = kind,
        mimeType = mimeType.takeIf { it.isNotBlank() }
            ?: error("Missing audio-only MIME type"),
        codec = codec,
        bitrate = bitrate,
        contentLength = contentLength?.takeIf { it >= 0L },
        durationMillis = duration?.takeIf { it >= 0L }?.times(1_000L),
    )
}
