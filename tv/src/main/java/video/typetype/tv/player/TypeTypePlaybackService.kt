@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package video.typetype.tv.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import video.typetype.sdk.core.Video
import video.typetype.sdk.core.SubtitleTrack
import video.typetype.sdk.core.StreamDetails
import video.typetype.sdk.core.AudioOnlyStream
import video.typetype.sdk.core.PlaybackSession
import video.typetype.sdk.core.UserSettings
import video.typetype.sdk.core.PlaybackSeekRequest
import video.typetype.sdk.core.TypeTypeClient
import video.typetype.sdk.core.TypeTypeResult
import video.typetype.sdk.media3.PlaybackMediaSourceHandle
import video.typetype.tv.BuildConfig
import video.typetype.tv.data.TypeTypeTvClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext

public class TypeTypePlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var session: MediaSession
    private lateinit var client: TypeTypeClient
    private var sourceHandle: PlaybackMediaSourceHandle? = null
    private val qualityMetrics = TvPlaybackQualityMetrics()
    private var currentRequest: TvPlaybackRequest? = null
    private var currentSubtitle: SubtitleTrack? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var rebuildingAfterSeek = false
    private var progressJob: Job? = null
    private var positionJob: Job? = null
    private var playbackJob: Job? = null
    private var seekJob: Job? = null
    private var lowerFormatRecoveryAttempts = 0
    private var sponsorBlockController: SponsorBlockController? = null
    override fun onCreate() {
        super.onCreate()
        client = TypeTypeTvClient.create(this, BuildConfig.TYPETYPE_INSTANCE_URL)
        player = createTvExoPlayer(this)
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) = handlePlayerError(error)

            override fun onPlaybackStateChanged(playbackState: Int) =
                qualityMetrics.onPlaybackStateChanged(playbackState)

            override fun onIsPlayingChanged(isPlaying: Boolean) =
                qualityMetrics.onIsPlayingChanged(isPlaying)

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK && !rebuildingAfterSeek) {
                    seek(newPosition.positionMs)
                }
            }
        })
        player.addAnalyticsListener(qualityMetrics)
        session = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = session

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_PLAY) play(intent)
        if (intent?.action == ACTION_STOP) stopPlayback()
        if (intent?.action == ACTION_SEEK) {
            seek(intent.getLongExtra(SEEK_TIME, 0L).coerceAtLeast(0L))
        }
        if (intent?.action == ACTION_RETRY) retryPlayback()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        sponsorBlockController?.stop()
        logSabrMetrics(sourceHandle)
        logTvPlaybackQuality(qualityMetrics)
        sourceHandle?.close()
        serviceScope.cancel()
        session.release()
        player.release()
        super.onDestroy()
    }

    private fun play(intent: Intent) {
        val request = intent.toPlaybackRequest() ?: return
        val subtitle = intent.toSubtitleTrack()
        sponsorBlockController?.stop()
        seekJob?.cancel()
        playbackJob?.cancel()
        playbackJob = serviceScope.launch {
            currentRequest = request
            lowerFormatRecoveryAttempts = 0
            currentSubtitle = subtitle
            qualityMetrics.onPlaybackRequested()
            player.setPlaybackSpeed(request.playbackSpeed)
            player.volume = request.playbackVolume
            replaceMediaSource(request, subtitle, request.startTimeMilliseconds, true)
            startSponsorBlock(request)
            startProgressUpdates()
            startPlaybackPositionUpdates()
        }
    }

    private fun stopPlayback() {
        sponsorBlockController?.stop()
        sponsorBlockController = null
        player.stop()
        player.clearMediaItems()
        logSabrMetrics(sourceHandle)
        logTvPlaybackQuality(qualityMetrics)
        sourceHandle?.close()
        sourceHandle = null
        currentRequest = null
        currentSubtitle = null
        progressJob?.cancel()
        positionJob?.cancel()
        playbackJob?.cancel()
        seekJob?.cancel()
        stopSelf()
    }

    private fun retryPlayback() {
        val request = currentRequest ?: return
        val position = player.currentPosition.coerceAtLeast(0L)
        playbackJob?.cancel()
        seekJob?.cancel()
        sponsorBlockController?.stop()
        lowerFormatRecoveryAttempts = 0
        playbackJob = schedulePlaybackRetry(
            scope = serviceScope,
            player = player,
            session = session,
            request = request,
            subtitle = currentSubtitle,
            positionMilliseconds = position,
            replace = ::replaceMediaSource,
            startSponsorBlock = ::startSponsorBlock,
            onStarted = {
                startProgressUpdates()
                startPlaybackPositionUpdates()
            },
        )
    }

    private fun handlePlayerError(error: PlaybackException) {
        val failure = error.findSabrPlaybackRestartRequired() ?: return
        val request = currentRequest ?: return
        if (lowerFormatRecoveryAttempts >= MAX_LOWER_FORMAT_RECOVERIES) return
        lowerFormatRecoveryAttempts++
        val position = player.currentPosition.coerceAtLeast(0L)
        playbackJob?.cancel()
        playbackJob = scheduleLowerFormatRecovery(
            scope = serviceScope,
            client = client,
            player = player,
            session = session,
            request = request,
            subtitle = currentSubtitle,
            failure = failure,
            positionMilliseconds = position,
            replace = ::replaceMediaSource,
            stopSponsorBlock = { sponsorBlockController?.stop() },
            startSponsorBlock = ::startSponsorBlock,
            onReplaced = { next ->
                currentRequest = next
                lowerFormatRecoveryAttempts = 0
            },
        )
    }

    private suspend fun replaceMediaSource(
        request: TvPlaybackRequest,
        subtitle: SubtitleTrack?,
        positionMilliseconds: Long,
        playWhenReady: Boolean,
    ) {
        val prepared = createTvPlaybackMediaSource(client, request, subtitle)
        val previous = sourceHandle
        try {
            player.setMediaSource(prepared.mediaSource, positionMilliseconds)
            sourceHandle = prepared.handle
            player.prepare()
            player.playWhenReady = playWhenReady
            session.setSessionExtras(
                Bundle().apply {
                    prepared.subtitleError?.let { putString(SUBTITLE_ERROR_EXTRA, it.toSubtitleMessage()) }
                },
            )
        } catch (failure: Exception) {
            prepared.handle.close()
            throw failure
        }
        logSabrMetrics(previous)
        previous?.close()
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scheduleProgressUpdates(serviceScope, client, player) { currentRequest }
    }

    private fun startPlaybackPositionUpdates() {
        positionJob?.cancel()
        positionJob = schedulePlaybackPositionUpdates(serviceScope, client, player) { currentRequest }
    }

    private fun seek(positionMilliseconds: Long) {
        val request = currentRequest ?: return
        if (request.isManifest || request.isAudioOnly) {
            player.seekTo(positionMilliseconds)
            return
        }
        seekJob?.cancel()
        seekJob = serviceScope.launch {
            val result = withContext(Dispatchers.IO) {
                client.playback.seek(
                    request.sessionId,
                    PlaybackSeekRequest(
                        positionMilliseconds = positionMilliseconds,
                        videoItag = request.videoItag,
                        audioItag = request.audioItag,
                        audioTrackId = request.audioTrackId,
                    ),
                )
            }
            if (result !is TypeTypeResult.Success) return@launch
            val session = result.value
            val next = request.copy(
                sessionId = session.sessionId,
                generation = session.generation,
                startTimeMilliseconds = positionMilliseconds,
            )
            val resumePlayback = player.playWhenReady
            sponsorBlockController?.stop()
            rebuildingAfterSeek = true
            try {
                replaceMediaSource(next, currentSubtitle, positionMilliseconds, resumePlayback)
                currentRequest = next
                startSponsorBlock(next)
            } finally {
                rebuildingAfterSeek = false
            }
        }
    }

    private fun startSponsorBlock(request: TvPlaybackRequest) {
        sponsorBlockController?.stop()
        sponsorBlockController = SponsorBlockController(
            serviceScope,
            player,
            request.sponsorBlockPolicy,
            request.playbackVolume,
            ::seek,
        ).also(SponsorBlockController::start)
    }

    public companion object {
        private const val MAX_LOWER_FORMAT_RECOVERIES = 2
        public fun play(
            context: Context,
            video: Video,
            stream: StreamDetails,
            session: PlaybackSession,
            subtitle: SubtitleTrack?,
            audioOnly: AudioOnlyStream?,
            settings: UserSettings,
        ): Boolean = TvPlaybackStarter.play(context, video, stream, session, subtitle, audioOnly, settings)

        public fun stop(context: Context) {
            context.startService(Intent(context, TypeTypePlaybackService::class.java).setAction(ACTION_STOP))
        }

        public fun seek(context: Context, positionMilliseconds: Long) {
            context.startService(
                Intent(context, TypeTypePlaybackService::class.java)
                    .setAction(ACTION_SEEK)
                    .putExtra(SEEK_TIME, positionMilliseconds.coerceAtLeast(0L)),
            )
        }

        public fun retry(context: Context) {
            context.startService(
                Intent(context, TypeTypePlaybackService::class.java).setAction(ACTION_RETRY),
            )
        }

        public fun controller(context: Context): ListenableFuture<MediaController> =
            MediaController.Builder(
                context,
                SessionToken(context, ComponentName(context, TypeTypePlaybackService::class.java)),
            ).buildAsync()
    }

}
