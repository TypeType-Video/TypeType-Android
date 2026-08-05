package dev.typetype.android.feature.player

import dev.typetype.android.domain.channel.ChannelRepository
import dev.typetype.android.domain.channel.ChannelQuery
import dev.typetype.android.domain.library.LibraryRepository
import dev.typetype.android.domain.library.VideoMeta
import dev.typetype.android.domain.library.VideoMetaRepository
import dev.typetype.android.domain.library.cacheVideos
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamRepository
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

data class LoadedStreamResult(
    val stream: Stream,
    val resumeAtMillis: Long,
)

sealed interface PlayerStreamUpdate {
    data class PlaybackReady(val loaded: LoadedStreamResult) : PlayerStreamUpdate
    data class MetadataEnriched(val stream: Stream) : PlayerStreamUpdate
    data class Failed(val failure: Throwable) : PlayerStreamUpdate
}

class PlayerStreamLoader @Inject constructor(
    private val streamRepository: StreamRepository,
    private val libraryRepository: LibraryRepository,
    private val videoMetaRepository: VideoMetaRepository,
    private val channelRepository: ChannelRepository,
) {
    private val metadataPrefetchCache = PlayerMetadataPrefetchCache()

    fun load(url: String): Flow<PlayerStreamUpdate> = loadProgressiveStream(
        loadPlayback = { streamRepository.loadPlaybackStream(url) },
        loadMetadata = { streamRepository.loadPlaybackMetadata(url) },
        loadChannelMetadata = { stream -> loadChannelMetadata(stream) },
        loadProgress = { libraryRepository.fetchProgressMillis(url).getOrNull() ?: 0L },
        prefetchedMetadata = metadataPrefetchCache.take(url),
    )

    suspend fun prefetchMetadata(url: String) {
        val metadata = streamRepository.loadPlaybackMetadata(url)?.getOrNull() ?: return
        metadataPrefetchCache.put(url, metadata)
    }

    private suspend fun loadChannelMetadata(stream: Stream): Result<Stream>? {
        if (stream.uploaderSubscriberCount >= 0L || stream.uploaderUrl.isBlank()) return null
        return channelRepository.loadChannel(ChannelQuery(stream.uploaderUrl)).map { page ->
            val channel = page.channel
            stream.copy(
                uploaderName = channel.name,
                uploaderAvatarUrl = channel.avatarUrl,
                uploaderSubscriberCount = channel.subscriberCount,
                uploaderVerified = channel.verified,
            )
        }
    }

    suspend fun cacheMetadata(videoUrl: String, stream: Stream) {
        videoMetaRepository.put(
            VideoMeta(
                videoUrl = videoUrl,
                channelName = stream.uploaderName,
                channelUrl = stream.uploaderUrl,
                channelAvatarUrl = stream.uploaderAvatarUrl,
                viewCount = stream.viewCount,
            ),
        )
        videoMetaRepository.cacheVideos(stream.relatedStreams)
    }

    suspend fun record(url: String, stream: Stream) = cacheMetadata(url, stream)
}

internal fun loadProgressiveStream(
    loadPlayback: suspend () -> Result<Stream>,
    loadMetadata: suspend () -> Result<Stream>?,
    loadChannelMetadata: suspend (Stream) -> Result<Stream>? = { null },
    loadProgress: suspend () -> Long,
    prefetchedMetadata: Stream? = null,
): Flow<PlayerStreamUpdate> = channelFlow {
    val savedProgress = async { loadProgress() }
    val metadata = if (prefetchedMetadata == null) async { loadMetadata() } else null
    loadPlayback().fold(
        onSuccess = { stream ->
            val loaded = LoadedStreamResult(
                stream = stream,
                resumeAtMillis = computeResumeMillis(
                    savedMillis = savedProgress.await(),
                    serverStartMillis = stream.startPositionMillis,
                    durationMillis = stream.durationSeconds * 1000L,
                ),
            )
            send(PlayerStreamUpdate.PlaybackReady(loaded))
            launch {
                loadChannelMetadata(stream)?.onSuccess { details ->
                    send(PlayerStreamUpdate.MetadataEnriched(stream.withMetadataFrom(details)))
                }
            }
            val prefetched = prefetchedMetadata?.takeIf {
                it.requestScope == stream.requestScope
            }
            val details = prefetched?.let { Result.success(it) }
                ?: metadata?.await()
                ?: loadMetadata()
            details?.onSuccess { details ->
                send(PlayerStreamUpdate.MetadataEnriched(stream.withMetadataFrom(details)))
            }
        },
        onFailure = { failure ->
            metadata?.cancel()
            send(PlayerStreamUpdate.Failed(failure))
        },
    )
}

private fun computeResumeMillis(
    savedMillis: Long,
    serverStartMillis: Long,
    durationMillis: Long,
): Long {
    val candidate = if (savedMillis > 0) savedMillis else serverStartMillis
    if (candidate < RESUME_MIN_MILLIS) return 0L
    if (durationMillis > 0 && candidate >= (durationMillis * RESUME_MAX_FRACTION).toLong()) return 0L
    return candidate
}

private const val RESUME_MIN_MILLIS = 5_000L
private const val RESUME_MAX_FRACTION = 0.95
