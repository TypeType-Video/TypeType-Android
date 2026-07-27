package dev.typetype.android

import dev.typetype.android.domain.feed.Video
import dev.typetype.android.domain.library.HistoryItem
import dev.typetype.android.domain.library.PlaylistVideo
import dev.typetype.android.domain.playback.PlaybackQueueEntry

internal fun PlaylistVideo.toPlaybackQueueEntry() = PlaybackQueueEntry(
    videoUrl = url,
    title = title,
    thumbnailUrl = thumbnailUrl,
    durationSeconds = durationSeconds,
    channelName = channelName,
)

internal fun Video.toPlaybackQueueEntry() = PlaybackQueueEntry(
    videoUrl = url,
    title = title,
    thumbnailUrl = thumbnailUrl,
    durationSeconds = durationSeconds,
    channelName = uploaderName,
)

internal fun HistoryItem.toPlaybackQueueEntry() = PlaybackQueueEntry(
    videoUrl = url,
    title = title,
    thumbnailUrl = thumbnailUrl,
    durationSeconds = durationSeconds,
    channelName = channelName,
)
