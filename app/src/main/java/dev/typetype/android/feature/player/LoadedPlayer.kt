package dev.typetype.android.feature.player

import android.graphics.Rect
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.paging.PagingData
import dev.typetype.android.domain.comments.Comment
import dev.typetype.android.domain.comments.CommentsRepository
import dev.typetype.android.domain.library.Playlist
import dev.typetype.android.domain.playback.PlaybackQueueState
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.domain.usersettings.UserSettings
import dev.typetype.android.feature.player.components.AutoplayCountdownOverlay
import dev.typetype.android.feature.player.components.AudioOnlyPlaybackDefault
import dev.typetype.android.feature.player.components.LocalMediaController
import dev.typetype.android.feature.player.components.PlayerGestureConfig
import dev.typetype.android.feature.player.components.PlayerSurfaceBox
import dev.typetype.android.feature.player.components.rememberPlaybackChapters
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

@Composable
fun LoadedPlayer(
    stream: Stream,
    videoUrl: String,
    resumeAtMillis: Long,
    initialPlayWhenReady: Boolean,
    playbackBindGeneration: Long,
    isFavorited: Boolean,
    isInWatchLater: Boolean,
    gestureConfig: PlayerGestureConfig,
    playbackBrightnessPercent: Int?,
    autoplayCountdownSeconds: Int,
    audioOnlyPlaybackDefault: Boolean?,
    preferredCodec: String,
    userSettings: UserSettings,
    playlists: List<Playlist>,
    playlistPickerVisible: Boolean,
    playlistActionInFlight: Boolean,
    downloadInFlight: Boolean,
    playbackQueue: PlaybackQueueState,
    commentsFlow: Flow<PagingData<Comment>>,
    commentsRepository: CommentsRepository?,
    prepareSabrPlayback: PrepareSabrPlayback,
    loadSubtitleCues: LoadSubtitleCues,
    isFullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenAccounts: () -> Unit,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenChannel: (channelUrl: String) -> Unit = {},
    isSubscribed: Boolean = false,
    subscriptionInFlight: Boolean = false,
    onToggleSubscription: () -> Unit = {},
    danmakuState: PlayerDanmakuState = PlayerDanmakuState(),
    onDanmakuAction: (PlayerDanmakuAction) -> Unit = {},
    onAction: (PlayerAction) -> Unit = {},
) {
    val controller = LocalMediaController.current
    val context = LocalContext.current
    val codecSupport = remember(context.applicationContext) {
        DevicePlaybackCodecSupport(context.applicationContext)
    }
    var commentsVisible by remember { mutableStateOf(false) }
    var downloadPickerVisible by remember { mutableStateOf(false) }
    var codecFallbackGeneration by remember(stream.id) { mutableLongStateOf(0L) }
    val selections = rememberPlayerPlaybackSelectionState(
        stream = stream,
        defaultQuality = userSettings.defaultQuality,
        defaultAudioLanguage = userSettings.defaultAudioLanguage,
        subtitlesEnabled = userSettings.subtitlesEnabled,
        defaultSubtitleLanguage = userSettings.defaultSubtitleLanguage,
        preferOriginalLanguage = userSettings.preferOriginalLanguage,
        defaultPlaybackSpeed = userSettings.defaultPlaybackSpeed,
        preferredCodec = preferredCodec,
    )
    val sponsorBlockPolicy = rememberSponsorBlockPlaybackPolicy(stream, userSettings)
    val playbackChapters = rememberPlaybackChapters(stream.chapters, sponsorBlockPolicy)
    var pipSourceRect by remember(stream.id) { mutableStateOf<Rect?>(null) }
    val activity = LocalActivity.current
    val autoplayCountdown = rememberPlayerAutoplayCountdown(
        player = controller,
        stream = stream,
        currentVideoUrl = videoUrl,
        playbackQueue = playbackQueue,
        enabled = userSettings.autoplay,
        countdownSeconds = autoplayCountdownSeconds,
        onAdvanceQueue = { onAction(PlayerAction.OnAdvanceQueue) },
        onCancelQueueAutoplay = { onAction(PlayerAction.OnCancelQueueAutoplay) },
        onToggleQueueAutoplayPause = {
            onAction(PlayerAction.OnToggleQueueAutoplayPause)
        },
        onPlayVideo = onPlayVideo,
    )

    LaunchedEffect(
        stream.id,
        stream.requestScope,
        controller,
        playbackBindGeneration,
        codecFallbackGeneration,
        selections.selectedCodec,
        selections.selectedQuality,
        selections.selectedAudioKey,
        userSettings.defaultAudioLanguage,
        userSettings.defaultQuality,
        userSettings.preferOriginalLanguage,
        initialPlayWhenReady,
    ) {
        controller?.let { ctrl ->
            bindStreamToController(
                controller = ctrl,
                stream = stream,
                videoUrl = videoUrl,
                startMillis = resumeAtMillis,
                selectedQuality = selections.selectedQuality,
                selectedAudioKey = selections.selectedAudioKey,
                selectedSubtitleKey = selections.selectedSubtitleKey,
                defaultAudioLanguage = userSettings.defaultAudioLanguage,
                automaticQualityCap = userSettings.defaultQuality,
                preferOriginalLanguage = userSettings.preferOriginalLanguage,
                initialPlayWhenReady = initialPlayWhenReady,
                codecSupport = codecSupport,
                prepareSabrPlayback = prepareSabrPlayback,
                selectedCodec = selections.selectedCodec,
            )
        }
    }

    LaunchedEffect(controller, selections.selectedSpeed) {
        controller?.setPlaybackSpeed(selections.selectedSpeed)
    }

    RuntimeCodecFallbackEffect(
        player = controller,
        enabled = selections.selectedCodec == RECOMMENDED_CODEC_KEY,
        codecSupport = codecSupport,
        onFallback = { codecFallbackGeneration += 1L },
    )

    PlayerSubtitleSelectionEffect(
        player = controller,
        selectedSubtitleKey = selections.selectedSubtitleKey,
    )

    PlayerProgressEffects(
        controller = controller,
        activity = activity,
        durationMillis = stream.durationSeconds * 1000L,
        audioOnlyAvailable = !stream.isLive && !stream.isLiveContent,
        pipSourceRect = pipSourceRect,
        onSaveProgress = { onAction(PlayerAction.OnSaveProgress(it)) },
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(
                if (isFullscreen) Modifier
                else Modifier.windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
                ),
            ),
    ) {
        PlayerContentLayout(
            isFullscreen = isFullscreen,
            viewport = { viewportModifier ->
                Box(
                    modifier = viewportModifier
                        .background(Color.Black)
                        .onGloballyPositioned { coordinates ->
                            val bounds = coordinates.boundsInWindow()
                            val updated = Rect(
                                bounds.left.roundToInt(),
                                bounds.top.roundToInt(),
                                bounds.right.roundToInt(),
                                bounds.bottom.roundToInt(),
                            )
                            if (updated != pipSourceRect) pipSourceRect = updated
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (controller != null) {
                        PlayerSurfaceBox(
                            player = controller,
                            stream = stream,
                            audioOnlyDefault = AudioOnlyPlaybackDefault(videoUrl, audioOnlyPlaybackDefault),
                            selectedCodec = selections.selectedCodec,
                            selectedQuality = selections.selectedQuality,
                            selectedAudioKey = selections.selectedAudioKey,
                            selectedSubtitleKey = selections.selectedSubtitleKey,
                            selectedSpeed = selections.selectedSpeed,
                            codecSupport = codecSupport,
                            onSelectCodec = {
                                selections.selectCodec(it)
                                onAction(PlayerAction.OnSetPreferredCodec(it))
                            },
                            onSelectQuality = selections::selectQuality,
                            onSelectAudio = selections::selectAudio,
                            onSelectSubtitle = selections::selectSubtitle,
                            onSelectSpeed = selections::selectSpeed,
                            onNavigateBack = {
                                if (isFullscreen) onFullscreenChange(false) else onNavigateBack()
                            },
                            onRetryPlayback = { onAction(PlayerAction.OnRetry) },
                            onOpenAccounts = onOpenAccounts,
                            pipSourceRect = pipSourceRect,
                            isFullscreen = isFullscreen,
                            onToggleFullscreen = { onFullscreenChange(!isFullscreen) },
                            sponsorBlockPolicy = sponsorBlockPolicy,
                            chapters = playbackChapters,
                            gestureConfig = gestureConfig,
                            playbackBrightnessPercent = playbackBrightnessPercent,
                            onPlaybackBrightnessChange = {
                                onAction(PlayerAction.OnSetPlaybackBrightness(it))
                            },
                            loadSubtitleCues = loadSubtitleCues,
                            captionStyles = userSettings.captionStyles,
                            danmakuState = danmakuState,
                            onDanmakuAction = onDanmakuAction,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                    autoplayCountdown?.let {
                        AutoplayCountdownOverlay(
                            state = it,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            },
            details = { detailsModifier ->
                PlayerDetails(
                    stream = stream,
                    videoUrl = videoUrl,
                    player = controller,
                    userSettings = userSettings,
                    playbackQueue = playbackQueue,
                    isFavorited = isFavorited,
                    isInWatchLater = isInWatchLater,
                    isSubscribed = isSubscribed,
                    subscriptionInFlight = subscriptionInFlight,
                    downloadInFlight = downloadInFlight,
                    onAction = onAction,
                    onShowComments = { commentsVisible = true },
                    onShowDownloads = { downloadPickerVisible = true },
                    onPlayVideo = onPlayVideo,
                    onOpenChannel = onOpenChannel,
                    onToggleSubscription = onToggleSubscription,
                    modifier = detailsModifier,
                )
            },
        )
    }

    PlayerAuxiliarySheets(
        commentsVisible = commentsVisible && !userSettings.hideComments,
        onDismissComments = { commentsVisible = false },
        commentsFlow = commentsFlow,
        commentsRepository = commentsRepository,
        videoUrl = videoUrl,
        controller = controller,
        playlistPickerVisible = playlistPickerVisible,
        playlists = playlists,
        playlistActionInFlight = playlistActionInFlight,
        downloadPickerVisible = downloadPickerVisible,
        downloadInFlight = downloadInFlight,
        onDismissDownload = { downloadPickerVisible = false },
        onAction = onAction,
    )
}
