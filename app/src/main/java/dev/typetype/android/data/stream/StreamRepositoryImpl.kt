package dev.typetype.android.data.stream

import dev.typetype.android.data.network.ApiBaseUrlHolder
import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.dto.AudioStreamItem
import dev.typetype.android.data.network.dto.SponsorBlockSegmentItem
import dev.typetype.android.data.network.dto.StreamResponse
import dev.typetype.android.data.network.dto.StreamSegmentItem
import dev.typetype.android.data.network.dto.SubtitleItem
import dev.typetype.android.data.network.dto.VideoItem
import dev.typetype.android.data.network.dto.VideoStreamItem
import dev.typetype.android.data.network.extractServerErrorMessage
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.stream.Chapter
import dev.typetype.android.domain.stream.SponsorAction
import dev.typetype.android.domain.stream.SponsorBlockSegment
import dev.typetype.android.domain.stream.SponsorCategory
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.stream.StreamAudioSource
import dev.typetype.android.domain.stream.StreamRepository
import dev.typetype.android.domain.stream.StreamSubtitleSource
import dev.typetype.android.domain.stream.StreamVideoSource
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Singleton
class StreamRepositoryImpl @Inject constructor(
    private val apiHolder: TypeTypeApiHolder,
    private val baseUrlHolder: ApiBaseUrlHolder,
) : StreamRepository {

    override suspend fun loadStream(videoUrl: String): Result<Stream> = runCatching {
        val api = apiHolder.require()
        val response = withContext(Dispatchers.IO) { api.streams(videoUrl) }
        if (!response.isSuccessful) {
            error(extractServerErrorMessage(response))
        }
        val body = response.body() ?: error("Empty stream body")
        body.toDomain(videoUrl)
    }

    private fun StreamResponse.toDomain(videoUrl: String): Stream = Stream(
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
        hlsUrl = hlsUrl.takeIf { it.isNotBlank() },
        dashMpdUrl = dashMpdUrl.takeIf { it.isNotBlank() },
        progressiveUrl = pickBestProgressiveStream(videoStreams),
        serverDashManifestUrl = serverManifestUrl("streams/manifest", videoUrl),
        serverHlsManifestUrl = hlsUrl.takeIf { it.isNotBlank() }
            ?.let { serverManifestUrl("streams/hls-manifest", videoUrl) },
        muxedVideoStreams = videoStreams
            .filter { !it.isVideoOnly }
            .map { it.toDomainVideoSource() },
        videoOnlyStreams = (videoOnlyStreams + videoStreams.filter { it.isVideoOnly })
            .distinctBy { it.url }
            .map { it.toDomainVideoSource() },
        audioStreams = audioStreams.map { it.toDomainAudioSource() },
        subtitles = subtitles.map { it.toDomainSubtitleSource() },
        startPositionMillis = startPosition * 1000L,
        sponsorBlockSegments = sponsorBlockSegments.map { it.toDomain() },
        chapters = streamSegments.map { it.toChapter() },
        relatedStreams = relatedStreams.map { it.toDomainVideo() },
    )

    private fun pickBestProgressiveStream(videoStreams: List<VideoStreamItem>): String? =
        videoStreams
            .filter { !it.isVideoOnly && it.url.isNotBlank() }
            .maxByOrNull { it.height }
            ?.url

    private fun serverManifestUrl(path: String, videoUrl: String): String? {
        val baseUrl = baseUrlHolder.currentBaseUrl?.trimEnd('/') ?: return null
        val encoded = URLEncoder.encode(videoUrl, StandardCharsets.UTF_8)
        return "$baseUrl/$path?url=$encoded"
    }

    private fun VideoStreamItem.toDomainVideoSource(): StreamVideoSource = StreamVideoSource(
        url = url,
        mimeType = mimeType,
        resolution = resolution,
        width = width,
        height = height,
        fps = fps,
        bitrate = bitrate,
        isVideoOnly = isVideoOnly,
    )

    private fun AudioStreamItem.toDomainAudioSource(): StreamAudioSource = StreamAudioSource(
        url = url,
        mimeType = mimeType,
        bitrate = bitrate,
        quality = quality,
        audioTrackId = audioTrackId,
        audioTrackName = audioTrackName,
        audioLocale = audioLocale,
        isOriginal = isOriginal,
    )

    private fun SubtitleItem.toDomainSubtitleSource(): StreamSubtitleSource = StreamSubtitleSource(
        url = proxiedVttUrl(url) ?: url,
        mimeType = "text/vtt",
        languageTag = languageTag,
        displayLanguageName = displayLanguageName,
        isAutoGenerated = isAutoGenerated,
    )

    private fun proxiedVttUrl(rawUrl: String): String? {
        val baseUrl = baseUrlHolder.currentBaseUrl?.trimEnd('/') ?: return null
        val vttUrl = rawUrl.toHttpUrlOrNull()
            ?.newBuilder()
            ?.setQueryParameter("fmt", "vtt")
            ?.build()
            ?.toString()
            ?: return null
        return "$baseUrl/proxy?url=${URLEncoder.encode(vttUrl, StandardCharsets.UTF_8)}"
    }

    private fun SponsorBlockSegmentItem.toDomain(): SponsorBlockSegment = SponsorBlockSegment(
        startMs = (startTime * 1_000).toLong(),
        endMs = (endTime * 1_000).toLong(),
        category = SponsorCategory.fromKey(category),
        action = SponsorAction.fromKey(action),
    )

    private fun StreamSegmentItem.toChapter(): Chapter = Chapter(
        title = title,
        startMs = startTimeSeconds.toLong() * 1_000L,
        previewUrl = previewUrl,
    )

    private fun VideoItem.toDomainVideo(): Video = Video(
        id = id,
        url = url,
        title = title,
        thumbnailUrl = thumbnailUrl,
        uploaderName = uploaderName,
        uploaderUrl = uploaderUrl,
        uploaderAvatarUrl = uploaderAvatarUrl,
        uploaderVerified = uploaderVerified,
        durationSeconds = duration,
        viewCount = viewCount,
        uploadedAtMillis = uploaded,
        isShortFormContent = isShortFormContent,
        shortDescription = shortDescription,
    )
}
