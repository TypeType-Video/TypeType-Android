package dev.typetype.android.services

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.domain.playback.PlaybackQueueEntry
import dev.typetype.android.domain.playback.PlaybackQueueMutationResult
import dev.typetype.android.domain.playback.PlaybackQueueState
import dev.typetype.android.domain.playback.PlaybackRepeatMode

internal fun Player.indexOfMediaId(mediaId: String): Int {
    for (index in 0 until mediaItemCount) {
        if (getMediaItemAt(index).mediaId == mediaId) return index
    }
    return -1
}

internal fun Player.retainCurrentMediaItem() {
    val current = currentMediaItemIndex
    if (current !in 0 until mediaItemCount) return
    if (current + 1 < mediaItemCount) removeMediaItems(current + 1, mediaItemCount)
    if (current > 0) removeMediaItems(0, current)
}

internal fun MediaItem.toPlaybackQueueEntry(): PlaybackQueueEntry? {
    val videoUrl = mediaId.takeIf(String::isNotBlank) ?: return null
    val extras = requestMetadata.extras ?: Bundle.EMPTY
    return PlaybackQueueEntry(
        videoUrl = videoUrl,
        title = extras.getString(MergedStreamMediaKeys.EXTRA_VIDEO_TITLE)
            ?: mediaMetadata.title?.toString().orEmpty(),
        thumbnailUrl = extras.getString(MergedStreamMediaKeys.EXTRA_VIDEO_THUMBNAIL)
            ?: mediaMetadata.artworkUri?.toString().orEmpty(),
        durationSeconds = extras.getLong(MergedStreamMediaKeys.EXTRA_VIDEO_DURATION_SECONDS),
        channelName = extras.getString(MergedStreamMediaKeys.EXTRA_CHANNEL_NAME)
            ?: mediaMetadata.artist?.toString().orEmpty(),
    )
}

internal data class PlaybackQueueAdoption(
    val state: PlaybackQueueState?,
    val owner: AccountScope?,
    val result: PlaybackQueueMutationResult,
)

internal fun Player.adoptQueue(entry: PlaybackQueueEntry): PlaybackQueueAdoption {
    if (playbackState == Player.STATE_ENDED) return noActiveQueueAdoption()
    val currentItem = currentMediaItem ?: return noActiveQueueAdoption()
    val playingEntry = currentItem.toPlaybackQueueEntry() ?: return noActiveQueueAdoption()
    if (playingEntry.videoUrl == entry.videoUrl) {
        return PlaybackQueueAdoption(null, null, PlaybackQueueMutationResult.AlreadyPlaying)
    }
    val scope = currentItem.requestMetadata.extras?.streamRequestScope()
    return PlaybackQueueAdoption(
        state = PlaybackQueueState(entries = listOf(playingEntry, entry), currentIndex = 0),
        owner = scope?.let { AccountScope(it.serverId, it.accountId) },
        result = PlaybackQueueMutationResult.Added,
    )
}

private fun noActiveQueueAdoption() = PlaybackQueueAdoption(
    state = null,
    owner = null,
    result = PlaybackQueueMutationResult.NoActivePlayback,
)

internal fun Player.applyQueueRepeatMode(queue: PlaybackQueueState) {
    val repeatCurrent = queue.repeatMode == PlaybackRepeatMode.One ||
        queue.repeatMode == PlaybackRepeatMode.All && queue.entries.size == 1
    repeatMode = if (repeatCurrent) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
}

internal fun Player.resumeQueueCycleIfNeeded(queue: PlaybackQueueState) {
    val shouldResume = playbackState == Player.STATE_ENDED && playWhenReady &&
        queue.repeatMode == PlaybackRepeatMode.All && queue.currentIndex == queue.entries.lastIndex
    if (!shouldResume) return
    seekToNextMediaItem()
    prepare()
    play()
}
