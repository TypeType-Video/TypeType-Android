package dev.typetype.android.services

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline

// Media3 moves a growing playlist to its live edge once the playlist is loaded, whatever start position it was given.
// A Twitch recording opened from its start, or from a saved position, seeks back there once its timeline is known.
internal class RecordingStartHolder(private val player: Player) : Player.Listener {
    private var heldMediaId: String? = null

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        val item = player.currentMediaItem ?: return
        val extras = item.requestMetadata.extras ?: return
        if (!extras.getBoolean(MergedStreamMediaKeys.EXTRA_HOLD_START_POSITION) || item.mediaId == heldMediaId) return
        if (timeline.isEmpty) return
        val window = timeline.getWindow(player.currentMediaItemIndex, Timeline.Window())
        if (window.isPlaceholder || !window.isDynamic) return
        heldMediaId = item.mediaId
        player.seekTo(extras.resumePositionMillis() ?: 0L)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (mediaItem?.mediaId != heldMediaId) heldMediaId = null
    }
}
