package dev.typetype.android.services

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dev.typetype.android.domain.playback.PlaybackResume
import dev.typetype.android.domain.playback.PlaybackResumeRepository
import dev.typetype.android.domain.usersettings.UserSettings
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal class PlaybackResumeRecorder(
    private val player: Player,
    private val repository: PlaybackResumeRepository,
    private val userSettingsRepository: UserSettingsRepository,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : Player.Listener, AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var identity = player.currentMediaItem?.resumeIdentity()
    private var persistenceEnabled = false
    private val periodicSave: Job

    init {
        player.addListener(this)
        scope.launch {
            userSettingsRepository.current().onSuccess { initial ->
                updatePersistencePolicy(initial)
                userSettingsRepository.observe().collect(::updatePersistencePolicy)
            }
        }
        periodicSave = scope.launch {
            while (true) {
                delay(SAVE_INTERVAL_MILLIS)
                if (player.isPlaying) saveCurrent()
            }
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val previous = identity
        identity = mediaItem?.resumeIdentity()
        if (mediaItem == null) {
            previous?.let(::delete)
            return
        }
        val startPosition = mediaItem.requestMetadata.extras?.resumePositionMillis()
        if (startPosition != null) save(startPosition)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (!isPlaying) saveCurrent()
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        save(newPosition.positionMs)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_READY -> saveCurrent()
            Player.STATE_ENDED -> clearCurrent()
            Player.STATE_IDLE,
            Player.STATE_BUFFERING,
            -> Unit
        }
    }

    override fun close() {
        player.removeListener(this)
        periodicSave.cancel()
        val finalIdentity = identity
        val finalPosition = player.currentPosition.coerceAtLeast(0L)
        if (finalIdentity == null || !persistenceEnabled) {
            scope.cancel()
        } else {
            scope.launch {
                repository.save(finalIdentity.toResume(finalPosition, nowMillis()))
                scope.cancel()
            }
        }
    }

    private fun saveCurrent() {
        save(player.currentPosition)
    }

    private fun save(positionMillis: Long) {
        if (!persistenceEnabled) return
        val current = identity ?: return
        val safePosition = positionMillis.coerceAtLeast(0L)
        scope.launch {
            repository.save(current.toResume(safePosition, nowMillis()))
        }
    }

    private fun clearCurrent() {
        val current = identity ?: return
        identity = null
        delete(current)
    }

    private fun updatePersistencePolicy(settings: UserSettings) {
        val wasEnabled = persistenceEnabled
        persistenceEnabled = !settings.disableWatchHistory
        when {
            !persistenceEnabled -> identity?.let(::delete)
            !wasEnabled -> saveCurrent()
        }
    }

    private fun delete(current: ResumeIdentity) {
        scope.launch {
            repository.clear(current.serverId, current.accountId)
        }
    }

    private data class ResumeIdentity(
        val serverId: String,
        val accountId: String,
        val videoUrl: String,
    ) {
        fun toResume(positionMillis: Long, updatedAtMillis: Long) = PlaybackResume(
            serverId = serverId,
            accountId = accountId,
            videoUrl = videoUrl,
            positionMillis = positionMillis,
            updatedAtMillis = updatedAtMillis,
        )
    }

    private fun MediaItem.resumeIdentity(): ResumeIdentity? {
        val scope = requestMetadata.extras?.streamRequestScope() ?: return null
        return mediaId.takeIf(String::isNotBlank)?.let { videoUrl ->
            ResumeIdentity(scope.serverId, scope.accountId, videoUrl)
        }
    }

    private companion object {
        const val SAVE_INTERVAL_MILLIS = 10_000L
    }
}
