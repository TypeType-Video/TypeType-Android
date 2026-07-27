package dev.typetype.android.services

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dev.typetype.android.domain.session.ActivePlaybackSnapshot
import dev.typetype.android.domain.session.ActiveSessionRepository
import dev.typetype.android.domain.stream.StreamRequestScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class ActivePlaybackReporter(
    private val player: Player,
    private val repository: ActiveSessionRepository,
) : Player.Listener, AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var startedIdentity: PlaybackIdentity? = null
    private var startJob: Job? = null
    private val progressJob: Job

    init {
        player.addListener(this)
        progressJob = scope.launch {
            while (true) {
                delay(PROGRESS_INTERVAL_MILLIS)
                reportProgress()
            }
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val next = mediaItem?.playbackIdentity()
        val previous = startedIdentity
        if (previous != null && previous != next) stop(previous)
        if (next == null || next.isLive) {
            startJob?.cancel()
            startJob = null
            startedIdentity = null
        } else if (player.isPlaying) {
            startIfNeeded()
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) startIfNeeded() else reportProgress()
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        reportProgress()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_READY -> if (player.isPlaying) startIfNeeded()
            Player.STATE_ENDED -> startedIdentity?.let(::stop)
            Player.STATE_IDLE,
            Player.STATE_BUFFERING,
            -> Unit
        }
    }

    override fun close() {
        player.removeListener(this)
        progressJob.cancel()
        startJob?.cancel()
        val identity = startedIdentity
        startedIdentity = null
        if (identity == null) {
            scope.cancel()
        } else {
            scope.launch {
                repository.reportPlaybackStop(identity.requestScope)
                scope.cancel()
            }
        }
    }

    private fun startIfNeeded() {
        val identity = player.currentMediaItem?.playbackIdentity()
            ?.takeUnless { it.isLive } ?: return
        if (startedIdentity == identity || startJob?.isActive == true) return
        val snapshot = player.snapshot(identity) ?: return
        startJob = scope.launch {
            repository.reportPlaybackStart(identity.requestScope, snapshot)
                .onSuccess {
                    if (player.currentMediaItem?.playbackIdentity() == identity) {
                        startedIdentity = identity
                    }
                }
        }
    }

    private fun reportProgress() {
        val identity = startedIdentity ?: return
        if (player.currentMediaItem?.playbackIdentity() != identity) return
        val snapshot = player.snapshot(identity) ?: return
        scope.launch {
            repository.reportPlaybackProgress(identity.requestScope, snapshot)
        }
    }

    private fun stop(identity: PlaybackIdentity) {
        if (startedIdentity == identity) startedIdentity = null
        scope.launch { repository.reportPlaybackStop(identity.requestScope) }
    }

    private fun Player.snapshot(identity: PlaybackIdentity): ActivePlaybackSnapshot? {
        val item = currentMediaItem ?: return null
        val title = item.mediaMetadata.title?.toString()?.takeIf(String::isNotBlank) ?: return null
        val duration = duration.takeIf { it != C.TIME_UNSET && it > 0L }
        return ActivePlaybackSnapshot(
            videoUrl = identity.videoUrl,
            title = title,
            thumbnailUrl = item.mediaMetadata.artworkUri?.toString(),
            channelName = item.mediaMetadata.artist?.toString(),
            positionMillis = currentPosition.coerceAtLeast(0L),
            durationMillis = duration,
            isPaused = !playWhenReady,
        )
    }

    private fun MediaItem.playbackIdentity(): PlaybackIdentity? {
        val extras = requestMetadata.extras ?: return null
        val requestScope = extras.streamRequestScope() ?: return null
        val videoUrl = mediaId.takeIf(String::isNotBlank) ?: return null
        return PlaybackIdentity(
            requestScope = requestScope,
            videoUrl = videoUrl,
            isLive = extras.getBoolean(MergedStreamMediaKeys.EXTRA_IS_LIVE_CONTENT),
        )
    }

    private data class PlaybackIdentity(
        val requestScope: StreamRequestScope,
        val videoUrl: String,
        val isLive: Boolean,
    )

    private companion object {
        const val PROGRESS_INTERVAL_MILLIS = 15_000L
    }
}
