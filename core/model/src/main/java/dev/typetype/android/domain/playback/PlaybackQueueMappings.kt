package dev.typetype.android.domain.playback

import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.library.HistoryItem
import dev.typetype.android.domain.library.PlaylistVideo

fun PlaylistVideo.toPlaybackQueueEntry() = PlaybackQueueEntry(
    videoUrl = url,
    title = title,
    thumbnailUrl = thumbnailUrl,
    durationSeconds = durationSeconds,
    channelName = channelName,
)

fun Video.toPlaybackQueueEntry() = PlaybackQueueEntry(
    videoUrl = url,
    title = title,
    thumbnailUrl = thumbnailUrl,
    durationSeconds = durationSeconds,
    channelName = uploaderName,
)

fun HistoryItem.toPlaybackQueueEntry() = PlaybackQueueEntry(
    videoUrl = url,
    title = title,
    thumbnailUrl = thumbnailUrl,
    durationSeconds = durationSeconds,
    channelName = channelName,
)
