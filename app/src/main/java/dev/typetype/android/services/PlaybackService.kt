package dev.typetype.android.services

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dev.typetype.android.MainActivity
import dev.typetype.android.R

@UnstableApi
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(buildNotificationProvider())
        val player = buildPlayer()
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(buildSessionActivityPendingIntent())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val session = mediaSession ?: return
        if (!session.player.playWhenReady || session.player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private fun buildPlayer(): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                BUFFER_FOR_PLAYBACK_MS,
                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
            )
            .setBackBuffer(BACK_BUFFER_MS, true)
            .build()
        return ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setLoadControl(loadControl)
            .setSeekBackIncrementMs(SEEK_BACK_INCREMENT_MS)
            .setSeekForwardIncrementMs(SEEK_FORWARD_INCREMENT_MS)
            .build()
    }

    private fun buildNotificationProvider(): DefaultMediaNotificationProvider =
        DefaultMediaNotificationProvider.Builder(this)
            .setNotificationId(NOTIFICATION_ID)
            .setChannelId(NOTIFICATION_CHANNEL_ID)
            .setChannelName(R.string.playback_channel_name)
            .build()
            .apply { setSmallIcon(R.drawable.ic_launcher_monochrome) }

    private fun buildSessionActivityPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private companion object {
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_CHANNEL_ID = "typetype.playback"
        const val MIN_BUFFER_MS = 30_000
        const val MAX_BUFFER_MS = 120_000
        const val BUFFER_FOR_PLAYBACK_MS = 2_500
        const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 5_000
        const val BACK_BUFFER_MS = 30_000
        const val SEEK_BACK_INCREMENT_MS = 10_000L
        const val SEEK_FORWARD_INCREMENT_MS = 10_000L
    }
}
