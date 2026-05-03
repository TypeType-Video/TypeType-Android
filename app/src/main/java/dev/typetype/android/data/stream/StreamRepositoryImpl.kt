package dev.typetype.android.data.stream

import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.StreamResponse
import dev.typetype.android.data.network.dto.VideoStreamItem
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class StreamRepositoryImpl @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
) : StreamRepository {

    override suspend fun loadStream(videoUrl: String): Result<Stream> = runCatching {
        val api = apiHolder.require()
        val response = withContext(Dispatchers.IO) { api.streams(videoUrl) }
        if (!response.isSuccessful) {
            error("Stream load failed (HTTP ${response.code()})")
        }
        val body = response.body() ?: error("Empty stream body")
        body.toDomain()
    }

    private fun StreamResponse.toDomain(): Stream = Stream(
        id = id,
        title = title,
        uploaderName = uploaderName,
        uploaderAvatarUrl = uploaderAvatarUrl,
        uploaderUrl = uploaderUrl,
        thumbnailUrl = thumbnailUrl,
        description = description,
        durationSeconds = duration,
        viewCount = viewCount,
        likeCount = likeCount,
        dislikeCount = dislikeCount,
        uploadedAtMillis = uploaded,
        hlsUrl = hlsUrl.takeIf { it.isNotBlank() },
        dashMpdUrl = dashMpdUrl.takeIf { it.isNotBlank() },
        progressiveUrl = pickBestProgressiveStream(videoStreams),
        startPositionMillis = startPosition,
    )

    private fun pickBestProgressiveStream(videoStreams: List<VideoStreamItem>): String? =
        videoStreams
            .filter { !it.isVideoOnly && it.url.isNotBlank() }
            .maxByOrNull { it.height }
            ?.url
}
