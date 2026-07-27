package dev.typetype.android.feature.player

import android.content.pm.ActivityInfo
import android.graphics.Rect
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import dev.typetype.android.core.ui.util.WindowHelper
import dev.typetype.android.domain.comments.Comment
import dev.typetype.android.domain.comments.CommentsRepository
import dev.typetype.android.domain.library.Playlist
import dev.typetype.android.domain.playback.PlaybackQueueState
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.feature.player.components.CommentsBar
import dev.typetype.android.feature.player.components.DescriptionSection
import dev.typetype.android.feature.player.components.LocalMediaController
import dev.typetype.android.feature.player.components.PlayerGestureConfig
import dev.typetype.android.feature.player.queue.PlaybackQueueControls
import dev.typetype.android.feature.player.sleep.PlaybackSleepTimerControls
import dev.typetype.android.feature.player.components.PlayerSurfaceBox
import dev.typetype.android.feature.player.components.RelatedStreamsSection
import dev.typetype.android.feature.player.components.UploaderCard
import dev.typetype.android.feature.menu.rememberVideoMenuScope
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
    autoplayEnabled: Boolean,
    defaultQuality: String,
    defaultAudioLanguage: String,
    subtitlesEnabled: Boolean,
    defaultSubtitleLanguage: String,
    preferOriginalLanguage: Boolean,
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
    onAction: (PlayerAction) -> Unit = {},
) {
    val controller = LocalMediaController.current
    val context = LocalContext.current
    val codecSupport = remember(context.applicationContext) {
        DevicePlaybackCodecSupport(context.applicationContext)
    }
    val scrollState = rememberScrollState()
    var commentsVisible by remember { mutableStateOf(false) }
    var downloadPickerVisible by remember { mutableStateOf(false) }
    var playbackBrightnessPercent by rememberSaveable(stream.id) {
        mutableStateOf<Int?>(null)
    }
    val selections = rememberPlayerPlaybackSelectionState(
        stream = stream,
        defaultQuality = defaultQuality,
        defaultAudioLanguage = defaultAudioLanguage,
        subtitlesEnabled = subtitlesEnabled,
        defaultSubtitleLanguage = defaultSubtitleLanguage,
        preferOriginalLanguage = preferOriginalLanguage,
    )
    var pipSourceRect by remember(stream.id) { mutableStateOf<Rect?>(null) }
    val videoMenuScope = rememberVideoMenuScope(onOpenChannel = onOpenChannel)
    val activity = LocalActivity.current

    LaunchedEffect(
        stream.id,
        stream.requestScope,
        controller,
        playbackBindGeneration,
        selections.selectedCodec,
        selections.selectedQuality,
        selections.selectedAudioKey,
        defaultAudioLanguage,
        defaultQuality,
        preferOriginalLanguage,
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
                defaultAudioLanguage = defaultAudioLanguage,
                automaticQualityCap = defaultQuality,
                preferOriginalLanguage = preferOriginalLanguage,
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

    PlayerSubtitleSelectionEffect(
        player = controller,
        selectedSubtitleKey = selections.selectedSubtitleKey,
    )

    PlayerProgressEffects(
        controller = controller,
        activity = activity,
        durationMillis = stream.durationSeconds * 1000L,
        autoplayEnabled = autoplayEnabled,
        explicitQueueActive = playbackQueue.isActive,
        nextVideoUrl = stream.relatedStreams.firstOrNull()?.url,
        pipSourceRect = pipSourceRect,
        onPlayVideo = onPlayVideo,
        onSaveProgress = { onAction(PlayerAction.OnSaveProgress(it)) },
    )

    LaunchedEffect(isFullscreen) {
        val window = activity?.window ?: return@LaunchedEffect
        if (isFullscreen) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            WindowHelper.toggleFullscreen(window, isFullscreen = true)
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            WindowHelper.toggleFullscreen(window, isFullscreen = false)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val window = activity?.window ?: return@onDispose
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            WindowHelper.toggleFullscreen(window, isFullscreen = false)
            onFullscreenChange(false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(
                if (isFullscreen) Modifier
                else Modifier.windowInsetsPadding(WindowInsets.statusBars),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isFullscreen) Modifier else Modifier.verticalScroll(scrollState)),
        ) {
            Box(
                modifier = (if (isFullscreen) {
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                }).onGloballyPositioned { coordinates ->
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
                        selectedCodec = selections.selectedCodec,
                        selectedQuality = selections.selectedQuality,
                        selectedAudioKey = selections.selectedAudioKey,
                        selectedSubtitleKey = selections.selectedSubtitleKey,
                        selectedSpeed = selections.selectedSpeed,
                        codecSupport = codecSupport,
                        onSelectCodec = selections::selectCodec,
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
                        sponsorBlockSegments = stream.sponsorBlockSegments,
                        chapters = stream.chapters,
                        gestureConfig = gestureConfig,
                        playbackBrightnessPercent = playbackBrightnessPercent,
                        onPlaybackBrightnessChange = { playbackBrightnessPercent = it },
                        loadSubtitleCues = loadSubtitleCues,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            if (!isFullscreen) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    DescriptionSection(
                        title = stream.title,
                        viewCount = stream.viewCount,
                        likeCount = stream.likeCount,
                        description = stream.description,
                        onTimestampClick = { controller?.seekTo(it) },
                    )
                    PlaybackQueueControls(playbackQueue)
                    PlaybackSleepTimerControls()
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    PlayerInteractionRow(
                        isFavorited = isFavorited,
                        isInWatchLater = isInWatchLater,
                        shareUrl = videoUrl,
                        onToggleFavorite = { onAction(PlayerAction.OnToggleFavorite) },
                        onToggleWatchLater = { onAction(PlayerAction.OnToggleWatchLater) },
                        onAddToPlaylist = { onAction(PlayerAction.OnOpenPlaylistPicker) },
                        onDownload = { downloadPickerVisible = true },
                        downloadInFlight = downloadInFlight,
                    )
                    UploaderCard(
                        name = stream.uploaderName,
                        avatarUrl = stream.uploaderAvatarUrl,
                        subscriberCount = stream.uploaderSubscriberCount,
                        verified = stream.uploaderVerified,
                        isSubscribed = isSubscribed,
                        subscriptionInFlight = subscriptionInFlight,
                        onCardClick = { onOpenChannel(stream.uploaderUrl) },
                        onSubscribeClick = onToggleSubscription,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    CommentsBar(onClick = { commentsVisible = true })
                    RelatedStreamsSection(
                        videos = stream.relatedStreams,
                        onPlayVideo = onPlayVideo,
                        menuScope = videoMenuScope,
                        onOpenChannel = onOpenChannel,
                    )
                }
            }
        }
    }

    PlayerAuxiliarySheets(
        commentsVisible = commentsVisible,
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
