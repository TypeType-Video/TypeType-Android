package dev.typetype.android.feature.player

import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import androidx.paging.PagingData
import dev.typetype.android.core.ui.util.WindowHelper
import dev.typetype.android.domain.comments.Comment
import dev.typetype.android.domain.comments.CommentsRepository
import dev.typetype.android.domain.library.Playlist
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.feature.player.components.CommentsBar
import dev.typetype.android.feature.player.components.CommentsSheet
import dev.typetype.android.feature.player.components.DescriptionSection
import dev.typetype.android.feature.player.components.LocalMediaController
import dev.typetype.android.feature.player.components.PlayerGestureConfig
import dev.typetype.android.feature.player.components.PlayerSurfaceBox
import dev.typetype.android.feature.player.components.PlaylistPickerSheet
import dev.typetype.android.feature.player.components.RelatedStreamsSection
import dev.typetype.android.feature.player.components.UploaderCard
import dev.typetype.android.feature.player.components.applyAutoEnterPipParams
import dev.typetype.android.feature.menu.rememberVideoMenuScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

@Composable
fun LoadedPlayer(
    stream: Stream,
    videoUrl: String,
    resumeAtMillis: Long,
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
    commentsFlow: Flow<PagingData<Comment>>,
    commentsRepository: CommentsRepository?,
    onNavigateBack: () -> Unit,
    onPlayVideo: (videoUrl: String) -> Unit,
    onOpenChannel: (channelUrl: String) -> Unit = {},
    onAction: (PlayerAction) -> Unit = {},
) {
    val controller = LocalMediaController.current
    val scrollState = rememberScrollState()
    var commentsVisible by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var selectedQuality by remember(stream.id, defaultQuality) {
        mutableStateOf(stream.initialQuality(defaultQuality))
    }
    var selectedAudioKey by remember(stream.id, defaultAudioLanguage, preferOriginalLanguage) {
        mutableStateOf(stream.initialAudioKey(defaultAudioLanguage, preferOriginalLanguage))
    }
    var selectedSubtitleKey by remember(stream.id, subtitlesEnabled, defaultSubtitleLanguage) {
        mutableStateOf(stream.initialSubtitleKey(subtitlesEnabled, defaultSubtitleLanguage))
    }
    var selectedSpeed by remember(stream.id) { mutableStateOf(1f) }
    val videoMenuScope = rememberVideoMenuScope(onOpenChannel = onOpenChannel)
    val activity = LocalActivity.current

    LaunchedEffect(
        stream.id,
        controller,
        selectedQuality,
        selectedAudioKey,
        selectedSubtitleKey,
        defaultAudioLanguage,
        defaultQuality,
        preferOriginalLanguage,
    ) {
        controller?.let { ctrl ->
            bindStreamToController(
                controller = ctrl,
                stream = stream,
                videoUrl = videoUrl,
                startMillis = resumeAtMillis,
                selectedQuality = selectedQuality,
                selectedAudioKey = selectedAudioKey,
                selectedSubtitleKey = selectedSubtitleKey,
                defaultAudioLanguage = defaultAudioLanguage,
                automaticQualityCap = defaultQuality,
                preferOriginalLanguage = preferOriginalLanguage,
            )
        }
    }

    LaunchedEffect(controller, selectedSpeed) {
        controller?.setPlaybackSpeed(selectedSpeed)
    }

    val durationMs = stream.durationSeconds * 1000L
    val saveProgressIfEligible: (Long) -> Unit = saveProgressIfEligible@{ positionMs ->
        if (positionMs < 5_000L) return@saveProgressIfEligible
        if (durationMs > 0 && positionMs >= (durationMs * 0.95).toLong()) return@saveProgressIfEligible
        onAction(PlayerAction.OnSaveProgress(positionMs))
    }

    LaunchedEffect(controller) {
        while (true) {
            delay(10_000)
            val ctrl = controller ?: continue
            if (ctrl.isPlaying) saveProgressIfEligible(ctrl.currentPosition)
        }
    }

    DisposableEffect(controller, autoplayEnabled, stream.relatedStreams) {
        val ctrl = controller
        if (ctrl == null) {
            onDispose { }
        } else {
            val nextUrl = stream.relatedStreams.firstOrNull()?.url
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED && autoplayEnabled && !nextUrl.isNullOrBlank()) {
                        onPlayVideo(nextUrl)
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    applyAutoEnterPipParams(activity, isPlaying)
                    if (!isPlaying) saveProgressIfEligible(ctrl.currentPosition)
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int,
                ) {
                    if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                        saveProgressIfEligible(newPosition.positionMs)
                    }
                }
            }
            ctrl.addListener(listener)
            applyAutoEnterPipParams(activity, ctrl.isPlaying)
            onDispose {
                ctrl.removeListener(listener)
                applyAutoEnterPipParams(activity, false)
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, controller) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                controller?.let { saveProgressIfEligible(it.currentPosition) }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
            controller?.let { ctrl -> saveProgressIfEligible(ctrl.currentPosition) }
            val window = activity?.window ?: return@onDispose
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            WindowHelper.toggleFullscreen(window, isFullscreen = false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
                modifier = if (isFullscreen) {
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                },
                contentAlignment = Alignment.Center,
            ) {
                if (controller != null) {
                    PlayerSurfaceBox(
                        player = controller,
                        stream = stream,
                        selectedQuality = selectedQuality,
                        selectedAudioKey = selectedAudioKey,
                        selectedSubtitleKey = selectedSubtitleKey,
                        selectedSpeed = selectedSpeed,
                        onSelectQuality = { selectedQuality = it },
                        onSelectAudio = { selectedAudioKey = it },
                        onSelectSubtitle = { selectedSubtitleKey = it },
                        onSelectSpeed = { selectedSpeed = it },
                        onNavigateBack = {
                            if (isFullscreen) isFullscreen = false else onNavigateBack()
                        },
                        isFullscreen = isFullscreen,
                        onToggleFullscreen = { isFullscreen = !isFullscreen },
                        sponsorBlockSegments = stream.sponsorBlockSegments,
                        chapters = stream.chapters,
                        gestureConfig = gestureConfig,
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
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DescriptionSection(
                        title = stream.title,
                        viewCount = stream.viewCount,
                        likeCount = stream.likeCount,
                        description = stream.description,
                    )
                    PlayerInteractionRow(
                        isFavorited = isFavorited,
                        isInWatchLater = isInWatchLater,
                        shareUrl = videoUrl,
                        onToggleFavorite = { onAction(PlayerAction.OnToggleFavorite) },
                        onToggleWatchLater = { onAction(PlayerAction.OnToggleWatchLater) },
                        onAddToPlaylist = { onAction(PlayerAction.OnOpenPlaylistPicker) },
                        onDownload = { onAction(PlayerAction.OnDownload(selectedQuality)) },
                        downloadInFlight = downloadInFlight,
                    )
                    UploaderCard(
                        name = stream.uploaderName,
                        avatarUrl = stream.uploaderAvatarUrl,
                        subscriberCount = stream.uploaderSubscriberCount,
                        verified = stream.uploaderVerified,
                        onCardClick = { onOpenChannel(stream.uploaderUrl) },
                    )
                    CommentsBar(onClick = { commentsVisible = true })
                    Spacer(Modifier.height(4.dp))
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

    if (commentsVisible && commentsRepository != null) {
        CommentsSheet(
            pagingFlow = commentsFlow,
            videoUrl = videoUrl,
            commentsRepository = commentsRepository,
            onDismiss = { commentsVisible = false },
        )
    }

    if (playlistPickerVisible) {
        PlaylistPickerSheet(
            playlists = playlists,
            isInFlight = playlistActionInFlight,
            onAddToPlaylist = { onAction(PlayerAction.OnAddToPlaylist(it)) },
            onCreatePlaylist = { onAction(PlayerAction.OnCreatePlaylistAndAdd(it)) },
            onDismiss = { onAction(PlayerAction.OnDismissPlaylistPicker) },
        )
    }
}
