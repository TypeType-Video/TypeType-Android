package dev.typetype.android.data.stream

import dev.typetype.android.data.network.TypeTypeApiHolder
import dev.typetype.android.data.network.extractServerErrorMessage
import dev.typetype.android.data.network.dto.SponsorBlockSegmentItem
import dev.typetype.android.data.network.dto.StreamResponse
import dev.typetype.android.data.network.dto.StreamSegmentItem
import dev.typetype.android.data.network.dto.VideoItem
import dev.typetype.android.data.network.dto.VideoStreamItem
import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.stream.Chapter
import dev.typetype.android.domain.stream.SponsorAction
import dev.typetype.android.domain.stream.SponsorBlockSegment
import dev.typetype.android.domain.stream.SponsorCategory
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
            error(extractServerErrorMessage(response))
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
        startPositionMillis = startPosition,
        sponsorBlockSegments = sponsorBlockSegments.map { it.toDomain() },
        chapters = streamSegments.map { it.toChapter() },
        relatedStreams = relatedStreams.map { it.toDomainVideo() },
    )

    private fun pickBestProgressiveStream(videoStreams: List<VideoStreamItem>): String? =
        videoStreams
            .filter { !it.isVideoOnly && it.url.isNotBlank() }
            .maxByOrNull { it.height }
            ?.url

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
