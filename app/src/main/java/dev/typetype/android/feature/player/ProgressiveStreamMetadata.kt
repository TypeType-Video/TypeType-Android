package dev.typetype.android.feature.player

import dev.typetype.android.domain.stream.Stream

internal fun Stream.withMetadataFrom(details: Stream): Stream = copy(
    title = details.title.preferNonBlank(title),
    uploaderName = details.uploaderName.preferNonBlank(uploaderName),
    uploaderAvatarUrl = details.uploaderAvatarUrl.preferNonBlank(uploaderAvatarUrl),
    uploaderUrl = details.uploaderUrl.preferNonBlank(uploaderUrl),
    uploaderSubscriberCount = details.uploaderSubscriberCount.preferKnown(uploaderSubscriberCount),
    uploaderVerified = uploaderVerified || details.uploaderVerified,
    thumbnailUrl = details.thumbnailUrl.preferNonBlank(thumbnailUrl),
    description = details.description.preferNonBlank(description),
    durationSeconds = details.durationSeconds.preferPositive(durationSeconds),
    viewCount = details.viewCount.preferKnown(viewCount),
    likeCount = details.likeCount.preferKnown(likeCount),
    dislikeCount = details.dislikeCount.preferKnown(dislikeCount),
    uploadedAtMillis = details.uploadedAtMillis.preferKnown(uploadedAtMillis),
    subtitles = subtitles.preferNonEmpty(details.subtitles),
    sponsorBlockSegments = details.sponsorBlockSegments.preferNonEmpty(sponsorBlockSegments),
    chapters = details.chapters.preferNonEmpty(chapters),
    relatedStreams = details.relatedStreams.preferNonEmpty(relatedStreams),
)

private fun String.preferNonBlank(fallback: String): String = ifBlank { fallback }

private fun Long.preferPositive(fallback: Long): Long = takeIf { it > 0L } ?: fallback

private fun Long.preferKnown(fallback: Long): Long = takeIf { it >= 0L } ?: fallback

private fun <T> List<T>.preferNonEmpty(fallback: List<T>): List<T> = ifEmpty { fallback }
