package dev.typetype.android.services

import android.os.SystemClock
import androidx.media3.common.Player

internal class SabrLivePlaybackFollower(private val player: Player) {
    private val follower = LivePlaybackFollower()

    fun transition(mediaId: String?) {
        follower.transition(mediaId)
    }

    fun observeSeek(positionMs: Long) {
        follower.observeSeek(
            currentMediaId = player.currentMediaItem?.mediaId,
            positionMs = positionMs,
            targetMs = player.currentLiveTargetPositionMs(),
        )
    }

    fun followEdge() {
        val mediaId = player.currentMediaItem?.mediaId
        val positionMs = player.currentPosition
        val targetMs = player.currentLiveTargetPositionMs()
        follower.initialize(mediaId, positionMs, targetMs)
        follower.nextTarget(
            currentMediaId = mediaId,
            positionMs = positionMs,
            targetMs = targetMs,
            playing = player.isPlaying,
            busy = player.playbackState != Player.STATE_READY || player.isLoading,
            nowMs = SystemClock.elapsedRealtime(),
        )?.let(player::seekTo)
    }
}
