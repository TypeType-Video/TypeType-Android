package dev.typetype.android.domain.stream

import dev.typetype.android.domain.feed.Video

data class Stream(
    val id: String,
    val title: String,
    val uploaderName: String,
    val uploaderAvatarUrl: String,
    val uploaderUrl: String,
    val uploaderSubscriberCount: Long,
    val uploaderVerified: Boolean,
    val thumbnailUrl: String,
    val description: String,
    val durationSeconds: Long,
    val viewCount: Long,
    val likeCount: Long,
    val dislikeCount: Long,
    val uploadedAtMillis: Long,
    val hlsUrl: String?,
    val dashMpdUrl: String?,
    val progressiveUrl: String?,
    val startPositionMillis: Long,
    val sponsorBlockSegments: List<SponsorBlockSegment> = emptyList(),
    val chapters: List<Chapter> = emptyList(),
    val relatedStreams: List<Video> = emptyList(),
)
