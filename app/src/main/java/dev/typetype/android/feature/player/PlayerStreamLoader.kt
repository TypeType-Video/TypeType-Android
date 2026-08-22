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
    private val channelMetadataCache = PlayerChannelMetadataCache()

    fun load(url: String): Flow<PlayerStreamUpdate> = loadProgressiveStream(
        loadPlayback = { streamRepository.loadPlaybackStream(url) },
        loadMetadata = { streamRepository.loadPlaybackMetadata(url) },
        loadChannelMetadata = { stream -> loadChannelMetadata(stream) },
        loadProgress = { libraryRepository.fetchProgressMillis(url).getOrNull() ?: 0L },
        prefetchedMetadata = metadataPrefetchCache.get(url),
    )

    suspend fun prefetchMetadata(url: String) {
        val metadata = streamRepository.loadPlaybackMetadata(url)?.getOrNull() ?: return
        metadataPrefetchCache.put(url, metadata)
    }

    suspend fun prefetchPlayback(url: String) {
        streamRepository.prefetchPlaybackStream(url).getOrNull()
    }

    private suspend fun loadChannelMetadata(stream: Stream): Result<Stream>? {
        if (stream.uploaderSubscriberCount >= 0L || stream.uploaderUrl.isBlank()) return null
        channelMetadataCache.get(stream.requestScope, stream.uploaderUrl)?.let { metadata ->
            return Result.success(stream.withChannelMetadata(metadata))
        }
        return channelRepository.loadChannel(ChannelQuery(stream.uploaderUrl))
            .map { page ->
                PlayerChannelMetadata(
                    name = page.channel.name,
                    avatarUrl = page.channel.avatarUrl,
                    subscriberCount = page.channel.subscriberCount,
                    verified = page.channel.verified,
                )
            }
            .onSuccess { metadata ->
                channelMetadataCache.put(stream.requestScope, stream.uploaderUrl, metadata)
            }
            .map(stream::withChannelMetadata)
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

private fun Stream.withChannelMetadata(metadata: PlayerChannelMetadata): Stream = copy(
    uploaderName = metadata.name,
    uploaderAvatarUrl = metadata.avatarUrl,
    uploaderSubscriberCount = metadata.subscriberCount,
    uploaderVerified = metadata.verified,
)

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
                val prefetched = prefetchedMetadata?.takeIf {
                    it.requestScope == stream.requestScope
                }
                val details = prefetched?.let { Result.success(it) }
                    ?: metadata?.await()
                    ?: loadMetadata()
                var enriched = stream
                details?.onSuccess { metadataStream ->
                    enriched = stream.withMetadataFrom(metadataStream)
                    send(PlayerStreamUpdate.MetadataEnriched(enriched))
                }
                if (enriched.needsChannelMetadata()) {
                    loadChannelMetadata(enriched)?.onSuccess { channelStream ->
                        send(
                            PlayerStreamUpdate.MetadataEnriched(
                                enriched.withMetadataFrom(channelStream),
                            ),
                        )
                    }
                }
            }
        },
        onFailure = { failure ->
            metadata?.cancel()
            send(PlayerStreamUpdate.Failed(failure))
        },
    )
}

private fun Stream.needsChannelMetadata(): Boolean =
    uploaderSubscriberCount < 0L && uploaderUrl.isNotBlank()

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
