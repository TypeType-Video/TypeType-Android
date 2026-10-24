package dev.typetype.android.services

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dev.typetype.android.domain.library.LibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class PlaybackHistoryRecorder(
    private val player: Player,
    private val repository: LibraryRepository,
) : Player.Listener, AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lastRecordedVideoUrl: String? = null

    init {
        player.addListener(this)
        player.currentMediaItem?.let(::record)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (mediaItem == null) {
            lastRecordedVideoUrl = null
            return
        }
        record(mediaItem)
    }

    override fun close() {
        player.removeListener(this)
        scope.cancel()
    }

    private fun record(mediaItem: MediaItem) {
        val item = mediaItem.historyItem() ?: return
        if (lastRecordedVideoUrl == item.videoUrl) return
        lastRecordedVideoUrl = item.videoUrl
        scope.launch {
            repository.addHistory(
                videoUrl = item.videoUrl,
                title = item.title,
                thumbnail = item.thumbnailUrl,
                duration = item.durationSeconds,
                channelName = item.channelName,
                channelUrl = item.channelUrl,
                channelAvatarUrl = item.channelAvatarUrl,
            )
        }
    }
}

private data class PlaybackHistoryItem(
    val videoUrl: String,
    val title: String,
    val thumbnailUrl: String,
    val durationSeconds: Long,
    val channelName: String,
    val channelUrl: String,
    val channelAvatarUrl: String,
)

private fun MediaItem.historyItem(): PlaybackHistoryItem? {
    val videoUrl = mediaId.takeIf(String::isNotBlank) ?: return null
    val extras = requestMetadata.extras ?: Bundle.EMPTY
    return PlaybackHistoryItem(
        videoUrl = videoUrl,
        title = extras.getString(MergedStreamMediaKeys.EXTRA_VIDEO_TITLE)
            ?: mediaMetadata.title?.toString().orEmpty(),
        thumbnailUrl = extras.getString(MergedStreamMediaKeys.EXTRA_VIDEO_THUMBNAIL)
            ?: mediaMetadata.artworkUri?.toString().orEmpty(),
        durationSeconds = extras.getLong(MergedStreamMediaKeys.EXTRA_VIDEO_DURATION_SECONDS),
        channelName = extras.getString(MergedStreamMediaKeys.EXTRA_CHANNEL_NAME)
            ?: mediaMetadata.artist?.toString().orEmpty(),
        channelUrl = extras.getString(MergedStreamMediaKeys.EXTRA_CHANNEL_URL).orEmpty(),
        channelAvatarUrl = extras.getString(MergedStreamMediaKeys.EXTRA_CHANNEL_AVATAR).orEmpty(),
    )
}
