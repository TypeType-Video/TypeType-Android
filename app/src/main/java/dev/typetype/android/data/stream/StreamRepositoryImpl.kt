package dev.typetype.android.data.stream

import dev.typetype.android.data.account.AccountScopeProvider
import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.data.network.PlaybackNetworkObserver
import dev.typetype.android.data.network.dto.AudioStreamItem
import dev.typetype.android.data.network.dto.SponsorBlockSegmentItem
import dev.typetype.android.data.network.dto.StreamResponse
import dev.typetype.android.data.network.dto.StreamSegmentItem
import dev.typetype.android.data.network.dto.SubtitleItem
import dev.typetype.android.data.network.dto.toDomainVideo
import dev.typetype.android.data.network.dto.VideoStreamItem
import dev.typetype.android.data.network.serverResponseException
import dev.typetype.android.domain.stream.Chapter
import dev.typetype.android.domain.stream.SponsorAction
import dev.typetype.android.domain.stream.SponsorBlockSegment
import dev.typetype.android.domain.stream.SponsorCategory
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamPlaybackContract
import dev.typetype.android.domain.stream.StreamAudioSource
import dev.typetype.android.domain.stream.StreamRepository
import dev.typetype.android.domain.stream.StreamRequestScope
import dev.typetype.android.domain.stream.StreamVideoSource
import dev.typetype.android.domain.stream.isServerSabrAudioFormat
import dev.typetype.android.domain.stream.isServerSabrVideoFormat
import dev.typetype.android.domain.server.ServerRepository
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Singleton
internal class StreamRepositoryImpl @Inject constructor(
    private val remoteSource: StreamRemoteSource,
    private val activeAccountScope: AccountScopeProvider,
    private val serverRepository: ServerRepository,
    private val networkMonitor: PlaybackNetworkObserver,
) : StreamRepository {
    private val playbackPrefetchCache = PlaybackStreamPrefetchCache()

    override suspend fun loadStream(videoUrl: String): Result<Stream> =
        load(videoUrl, playbackBootstrap = false)

    override suspend fun loadPlaybackStream(videoUrl: String): Result<Stream> {
        val cached = cancellableStreamResult { prefetchedPlayback(videoUrl) }
            .getOrElse { return Result.failure(it) }
        return cached?.let(Result.Companion::success)
            ?: load(videoUrl, playbackBootstrap = true)
    }

    override suspend fun prefetchPlaybackStream(videoUrl: String): Result<Stream> {
        val cached = cancellableStreamResult { prefetchedPlayback(videoUrl) }
            .getOrElse { return Result.failure(it) }
        if (cached != null) return Result.success(cached)
        return load(videoUrl, playbackBootstrap = true).map { stream ->
            if (!stream.isLive && !stream.isLiveContent) {
                playbackPrefetchCache.put(videoUrl, stream)
            }
            stream
        }
    }

    override suspend fun loadPlaybackMetadata(videoUrl: String): Result<Stream>? =
        if (videoUrl.streamProvider() == StreamProvider.YouTube) loadStream(videoUrl) else null

    private suspend fun prefetchedPlayback(videoUrl: String): Stream? {
        val account = activeAccountScope.require()
        val server = serverRepository.getServer(account.serverId) ?: error("Instance not found")
        val requestScope = StreamRequestScope(
            serverId = account.serverId,
            accountId = account.accountId,
            baseUrl = server.baseUrl,
        )
        val cached = playbackPrefetchCache.get(videoUrl, requestScope)
        activeAccountScope.verify(account)
        return cached
    }

    private suspend fun load(
        videoUrl: String,
        playbackBootstrap: Boolean,
    ): Result<Stream> = cancellableStreamResult {
        val scope = activeAccountScope.require()
        val server = serverRepository.getServer(scope.serverId) ?: error("Instance not found")
        val provider = videoUrl.streamProvider()
        val response = withContext(Dispatchers.IO) {
            transientPlaybackRequest(
                pause = { delay(it) },
                network = networkMonitor,
            ) {
                remoteSource.load(scope, videoUrl, provider, playbackBootstrap)
            }
        }
        if (!response.isSuccessful) {
            throw serverResponseException(response)
        }
        val body = response.body() ?: error("Empty stream body")
        activeAccountScope.verify(scope)
        if (provider == StreamProvider.YouTube && !body.hasPlayableSabrContract(server.baseUrl)) {
            throw SabrContractException()
        }
        body.toDomain(videoUrl, server.baseUrl, scope, provider)
    }

    private fun StreamResponse.toDomain(
        videoUrl: String,
        baseUrl: String,
        scope: dev.typetype.android.data.account.AccountScope,
        provider: StreamProvider,
    ): Stream {
        val serverSabr = provider == StreamProvider.YouTube
        return Stream(
            playbackContract = if (serverSabr) {
                StreamPlaybackContract.ServerSabr
            } else {
                StreamPlaybackContract.ProviderMedia
            },
            id = id,
            title = title,
            uploaderName = uploaderName,
            uploaderAvatarUrl = uploaderAvatarUrl,
            uploaderUrl = uploaderUrl,
            uploaderSubscriberCount = uploaderSubscriberCount,
            uploaderVerified = uploaderVerified,
            thumbnailUrl = thumbnailUrl,
            description = description,
            durationSeconds = duration,
            viewCount = viewCount,
            likeCount = likeCount,
            dislikeCount = dislikeCount,
            uploadedAtMillis = uploaded,
            hlsUrl = hlsUrl.takeIf { !serverSabr && it.isNotBlank() },
            dashMpdUrl = dashMpdUrl.takeIf { !serverSabr && it.isNotBlank() },
            progressiveUrl = pickBestProgressiveStream(videoStreams).takeUnless { serverSabr },
            serverDashManifestUrl = serverManifestUrl(baseUrl, "streams/manifest", videoUrl)
                .takeUnless { serverSabr },
            serverHlsManifestUrl = hlsUrl.takeIf { !serverSabr && it.isNotBlank() }
                ?.let { serverManifestUrl(baseUrl, "streams/hls-manifest", videoUrl) },
            serverSabrManifestUrl = resolveServerUrl(baseUrl, firstSabrManifestUrl()),
            sabrVideoStreams = (videoOnlyStreams + videoStreams)
                .filter {
                    it.deliveryMethod == SABR_DELIVERY_METHOD && it.itag > 0 &&
                        isServerSabrVideoFormat(it.codec)
                }
                .map { it.toDomainVideoSource(baseUrl) },
            sabrAudioStreams = audioStreams
                .filter {
                    it.deliveryMethod == SABR_DELIVERY_METHOD && it.itag > 0 &&
                        isServerSabrAudioFormat(it.mimeType, it.codec)
                }
                .map { it.toDomainAudioSource(baseUrl) },
            requestScope = StreamRequestScope(scope.serverId, scope.accountId, baseUrl),
            muxedVideoStreams = videoStreams
                .takeUnless { serverSabr }
                .orEmpty()
                .filter { !it.isVideoOnly && it.deliveryMethod != SABR_DELIVERY_METHOD }
                .map { it.toDomainVideoSource(baseUrl) },
            videoOnlyStreams = (videoOnlyStreams + videoStreams.filter { it.isVideoOnly })
                .takeUnless { serverSabr }
                .orEmpty()
                .filter { it.deliveryMethod != SABR_DELIVERY_METHOD }
                .distinctBy { it.url }
                .map { it.toDomainVideoSource(baseUrl) },
            audioStreams = audioStreams
                .takeUnless { serverSabr }
                .orEmpty()
                .filter { it.deliveryMethod != SABR_DELIVERY_METHOD }
                .map { it.toDomainAudioSource(baseUrl) },
            originalAudioTrackId = originalAudioTrackId,
            preferredDefaultAudioTrackId = preferredDefaultAudioTrackId,
            subtitles = subtitles.mapIndexedNotNull { index, subtitle ->
                subtitle.toClientSubtitleSource(index)
            },
            startPositionMillis = startPosition * 1000L,
            sponsorBlockSegments = sponsorBlockSegments.mapNotNull {
                it.toDomainSponsorBlockSegment(duration)
            },
            chapters = streamSegments.map { it.toChapter() },
            relatedStreams = relatedStreams.map { it.toDomainVideo() },
            isLive = isLive,
            isPostLive = isPostLive,
            isLiveContent = isLiveContent,
            category = category,
        )
    }

    private fun StreamResponse.firstSabrManifestUrl(): String? =
        (videoOnlyStreams + videoStreams).firstNotNullOfOrNull { item ->
            item.manifestUrl.takeIf { item.deliveryMethod == SABR_DELIVERY_METHOD }
        } ?: audioStreams.firstNotNullOfOrNull { item ->
            item.manifestUrl.takeIf { item.deliveryMethod == SABR_DELIVERY_METHOD }
        }

    private fun pickBestProgressiveStream(videoStreams: List<VideoStreamItem>): String? =
        videoStreams
            .filter { !it.isVideoOnly && it.url.isNotBlank() }
            .maxByOrNull { it.height }
            ?.url

    private fun serverManifestUrl(baseUrl: String, path: String, videoUrl: String): String {
        val normalizedBaseUrl = baseUrl.trimEnd('/')
        val encoded = URLEncoder.encode(videoUrl, StandardCharsets.UTF_8)
        return "$normalizedBaseUrl/$path?url=$encoded"
    }

    private fun VideoStreamItem.toDomainVideoSource(baseUrl: String): StreamVideoSource = StreamVideoSource(
        url = if (deliveryMethod == SABR_DELIVERY_METHOD) resolveServerUrl(baseUrl, manifestUrl).orEmpty() else url,
        mimeType = mimeType,
        codec = codec,
        resolution = resolution,
        width = width,
        height = height,
        fps = fps,
        bitrate = bitrate,
        isVideoOnly = isVideoOnly,
        itag = itag,
    )

    private fun AudioStreamItem.toDomainAudioSource(baseUrl: String): StreamAudioSource = StreamAudioSource(
        url = if (deliveryMethod == SABR_DELIVERY_METHOD) resolveServerUrl(baseUrl, manifestUrl).orEmpty() else url,
        mimeType = mimeType,
        codec = codec,
        bitrate = bitrate,
        quality = quality,
        audioTrackId = audioTrackId,
        audioTrackName = audioTrackName,
        audioLocale = audioLocale,
        isOriginal = isOriginal,
        itag = itag,
    )

    private fun StreamSegmentItem.toChapter(): Chapter = Chapter(
        title = title,
        startMs = startTimeSeconds.toLong() * 1_000L,
        previewUrl = previewUrl,
    )

}

internal fun SponsorBlockSegmentItem.toDomainSponsorBlockSegment(
    durationSeconds: Long,
): SponsorBlockSegment? {
    if (!startTime.isFinite() || !endTime.isFinite()) return null
    val scale = if (durationSeconds > 0L && endTime > durationSeconds + 30L) 1.0 else 1_000.0
    val startMs = (startTime * scale).roundToLong().coerceAtLeast(0L)
    val endMs = (endTime * scale).roundToLong().coerceAtLeast(0L)
    if (endMs <= startMs) return null
    return SponsorBlockSegment(
        startMs = startMs,
        endMs = endMs,
        category = SponsorCategory.fromKey(category),
        action = SponsorAction.fromKey(action),
    )
}

internal suspend fun <T> cancellableStreamResult(
    request: suspend () -> T,
): Result<T> = try {
    Result.success(request())
} catch (failure: CancellationException) {
    throw failure
} catch (failure: Throwable) {
    Result.failure(failure)
}

private const val SABR_DELIVERY_METHOD = "sabr"

private class SabrContractException :
    IllegalStateException("The server returned no playable SABR contract"),
    CodedFailure {
    override val failureCode: String = "youtube_sabr_unavailable"
    override val requestId: String? = null
    override val statusCode: Int? = null
}
