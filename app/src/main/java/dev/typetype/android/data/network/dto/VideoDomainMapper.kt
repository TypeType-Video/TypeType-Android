package dev.typetype.android.data.network.dto

import dev.typetype.android.domain.feed.Video

fun VideoItem.toDomainVideo(): Video = Video(
    id = id,
    url = url,
    title = title,
    thumbnailUrl = thumbnailUrl,
    uploaderName = uploaderName,
    uploaderUrl = uploaderUrl,
    uploaderAvatarUrl = uploaderAvatarUrl,
    uploaderVerified = uploaderVerified,
    durationSeconds = duration,
    isLive = isLive || streamType == "live_stream" || streamType == "audio_live_stream",
    viewCount = viewCount,
    uploadedAtMillis = uploaded,
    isShortFormContent = isShortFormContent,
    shortDescription = shortDescription,
    publishedAtMillis = publishedAt,
    isPostLive = isPostLive,
    isLiveContent = isLiveContent,
    requiresMembership = requiresMembership,
)
