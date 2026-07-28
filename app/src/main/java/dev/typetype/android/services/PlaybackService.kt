package dev.typetype.android.services

import android.app.PendingIntent
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dev.typetype.android.MainActivity
import dev.typetype.android.R
import dev.typetype.android.domain.stream.SabrPlaybackRepository
import dev.typetype.android.data.network.PlaybackNetworkMonitor
import dev.typetype.android.domain.playback.PlaybackResumeRepository
import dev.typetype.android.domain.library.LibraryRepository
import dev.typetype.android.domain.session.ActiveSessionRepository
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var mediaClientFactory: ScopedMediaClientFactory

    @Inject
    lateinit var sabrPlaybackRepository: SabrPlaybackRepository

    @Inject
    lateinit var sabrPlaybackWindowCache: SabrPlaybackWindowCache

    @Inject
    lateinit var playbackResumeRepository: PlaybackResumeRepository

    @Inject
    lateinit var userSettingsRepository: UserSettingsRepository

    @Inject
    lateinit var activeSessionRepository: ActiveSessionRepository

    @Inject
    lateinit var libraryRepository: LibraryRepository

    @Inject
    lateinit var playbackQueueCoordinator: PlaybackQueueCoordinator

    @Inject
    lateinit var playbackSleepTimer: PlaybackSleepTimer

    @Inject
    lateinit var playbackNetworkMonitor: PlaybackNetworkMonitor

    private var mediaSession: MediaSession? = null
    private var sabrPlaybackBridge: SabrPlaybackServiceBridge? = null
    private var playbackResumeRecorder: PlaybackResumeRecorder? = null
    private var activePlaybackReporter: ActivePlaybackReporter? = null
    private var playbackHistoryRecorder: PlaybackHistoryRecorder? = null
    private var playbackPlayer: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(buildNotificationProvider())
        val playbackClock = SabrPlaybackClock()
        val recoveryDispatcher = SabrPlaybackRecoveryDispatcher()
        val player = buildPlayer(playbackClock, recoveryDispatcher)
        playbackPlayer = player
        val playbackBridge = SabrPlaybackServiceBridge(
            player,
            sabrPlaybackRepository,
            sabrPlaybackWindowCache,
            playbackClock,
            recoveryDispatcher,
            playbackNetworkMonitor,
        )
        sabrPlaybackBridge = playbackBridge
        playbackResumeRecorder = PlaybackResumeRecorder(
            player,
            playbackResumeRepository,
            userSettingsRepository,
        )
        activePlaybackReporter = ActivePlaybackReporter(player, activeSessionRepository)
        playbackHistoryRecorder = PlaybackHistoryRecorder(player, libraryRepository)
        playbackQueueCoordinator.attach(player)
        playbackSleepTimer.attach(player)
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(buildSessionActivityPendingIntent())
            .setMediaButtonPreferences(buildMediaButtonPreferences())
            .setCallback(
                PlaybackSessionCallback(
                    packageName,
                    applicationInfo.uid,
                    playbackBridge,
                ),
            )
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
        activePlaybackReporter?.close()
        activePlaybackReporter = null
        playbackHistoryRecorder?.close()
        playbackHistoryRecorder = null
        playbackPlayer?.let(playbackSleepTimer::detach)
        mediaSession?.player?.let(playbackQueueCoordinator::detach)
        playbackResumeRecorder?.close()
        playbackResumeRecorder = null
        sabrPlaybackBridge?.close()
        sabrPlaybackBridge = null
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        playbackPlayer = null
        super.onDestroy()
    }

    private fun buildPlayer(
        playbackClock: SabrPlaybackClock,
        recoveryDispatcher: SabrPlaybackRecoveryDispatcher,
    ): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
        val renderersFactory = DefaultRenderersFactory(this)
            .setEnableDecoderFallback(true)
        return ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setLoadControl(createPlaybackLoadControl())
            .setMediaSourceFactory(
                MergedStreamMediaSourceFactory(
                    this,
                    mediaClientFactory,
                    sabrPlaybackRepository,
                    sabrPlaybackWindowCache,
                    playbackClock::currentPositionUs,
                    playbackClock::currentPlaybackRate,
                    recoveryDispatcher,
                    playbackNetworkMonitor,
                )
                    .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(MIN_LOADABLE_RETRY_COUNT)),
            )
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

    private fun buildMediaButtonPreferences(): List<CommandButton> =
        listOf(
            CommandButton.Builder(CommandButton.ICON_SKIP_BACK_10)
                .setDisplayName(getString(R.string.player_rewind))
                .setPlayerCommand(Player.COMMAND_SEEK_BACK)
                .setSlots(CommandButton.SLOT_BACK, CommandButton.SLOT_OVERFLOW)
                .build(),
            CommandButton.Builder(CommandButton.ICON_PLAY)
                .setDisplayName(getString(R.string.player_play_pause))
                .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
                .setSlots(CommandButton.SLOT_CENTRAL)
                .build(),
            CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD_10)
                .setDisplayName(getString(R.string.player_forward))
                .setPlayerCommand(Player.COMMAND_SEEK_FORWARD)
                .setSlots(CommandButton.SLOT_FORWARD, CommandButton.SLOT_OVERFLOW)
                .build(),
            CommandButton.Builder(CommandButton.ICON_NEXT)
                .setDisplayName(getString(R.string.playback_queue_next))
                .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .setSlots(CommandButton.SLOT_FORWARD_SECONDARY, CommandButton.SLOT_OVERFLOW)
                .build(),
        )

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
        const val MIN_LOADABLE_RETRY_COUNT = 5
        const val SEEK_BACK_INCREMENT_MS = 10_000L
        const val SEEK_FORWARD_INCREMENT_MS = 10_000L
    }
}
