package dev.typetype.android.feature.player

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import dev.typetype.android.R
import dev.typetype.android.domain.stream.StreamPlaybackContract
import dev.typetype.android.feature.player.components.LocalMediaController
import dev.typetype.android.feature.player.components.PlaybackOptionsSheet
import dev.typetype.android.feature.player.components.PlaybackKeepScreenOnEffect
import dev.typetype.android.feature.player.components.PlayerSubtitleOverlay
import dev.typetype.android.feature.player.components.ResilientPlayerSurface
import dev.typetype.android.feature.player.components.SponsorBlockPlaybackFeedback
import dev.typetype.android.feature.player.components.rememberCurrentMediaId
import dev.typetype.android.feature.player.components.rememberPlayerPlaybackStatus
import dev.typetype.android.feature.player.state.ResizeMode

@Composable
fun ShortsPlayerRoute(
    videoUrl: String,
    viewModel: PlayerViewModel,
    onAdvance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            state.videoUrl != videoUrl || state.isLoading -> CircularProgressIndicator()
            state.error != null || state.stream == null -> FilledIconButton(
                onClick = { viewModel.onAction(PlayerAction.OnRetry) },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = stringResource(R.string.action_retry),
                )
            }
            else -> ShortsPlayerSurface(
                state = state,
                prepareSabrPlayback = viewModel.sabrPlayback::prepare,
                loadSubtitleCues = viewModel.subtitleCueLoader::load,
                onAdvance = onAdvance,
                onRetry = { viewModel.onAction(PlayerAction.OnRetry) },
                onSaveProgress = {
                    viewModel.onAction(PlayerAction.OnSaveProgress(it))
                },
            )
        }
    }
}

@Composable
private fun ShortsPlayerSurface(
    state: PlayerState,
    prepareSabrPlayback: PrepareSabrPlayback,
    loadSubtitleCues: LoadSubtitleCues,
    onAdvance: () -> Unit,
    onRetry: () -> Unit,
    onSaveProgress: (Long) -> Unit,
) {
    val stream = requireNotNull(state.stream)
    val controller = LocalMediaController.current
    val context = LocalContext.current
    val activity = LocalActivity.current
    val codecSupport = remember(context.applicationContext) {
        DevicePlaybackCodecSupport(context.applicationContext)
    }
    val selections = rememberPlayerPlaybackSelectionState(
        stream = stream,
        defaultQuality = state.userSettings.defaultQuality,
        defaultAudioLanguage = state.userSettings.defaultAudioLanguage,
        subtitlesEnabled = state.userSettings.subtitlesEnabled,
        defaultSubtitleLanguage = state.userSettings.defaultSubtitleLanguage,
        preferOriginalLanguage = state.userSettings.preferOriginalLanguage,
        defaultPlaybackSpeed = state.userSettings.defaultPlaybackSpeed,
    )
    val sponsorBlockPolicy = rememberSponsorBlockPlaybackPolicy(stream, state.userSettings)
    var playbackOptionsVisible by remember(stream.id) { mutableStateOf(false) }
    var resizeMode by remember(stream.id) { mutableStateOf(ResizeMode.Crop) }
    val playbackStatus = controller?.let {
        rememberPlayerPlaybackStatus(
            it,
            onRetry.takeIf { stream.playbackContract == StreamPlaybackContract.ServerSabr },
        )
    }
    val currentMediaId = rememberCurrentMediaId(controller)
    val externalSubtitle = stream.subtitles.firstOrNull {
        stream.playbackContract == StreamPlaybackContract.ServerSabr &&
            it.key == selections.selectedSubtitleKey
    }

    LaunchedEffect(
        controller,
        stream.id,
        stream.requestScope,
        state.playbackBindGeneration,
        selections.selectedCodec,
        selections.selectedQuality,
        selections.selectedAudioKey,
        selections.selectedSubtitleKey,
        state.initialPlayWhenReady,
    ) {
        val player = controller ?: return@LaunchedEffect
        bindStreamToController(
            controller = player,
            stream = stream,
            videoUrl = state.videoUrl,
            startMillis = state.resumeAtMillis,
            selectedQuality = selections.selectedQuality,
            selectedAudioKey = selections.selectedAudioKey,
            selectedSubtitleKey = selections.selectedSubtitleKey,
            defaultAudioLanguage = state.userSettings.defaultAudioLanguage,
            automaticQualityCap = state.userSettings.defaultQuality,
            preferOriginalLanguage = state.userSettings.preferOriginalLanguage,
            initialPlayWhenReady = state.initialPlayWhenReady,
            codecSupport = codecSupport,
            prepareSabrPlayback = prepareSabrPlayback,
            selectedCodec = selections.selectedCodec,
        )
        player.setPlaybackSpeed(selections.selectedSpeed)
    }

    PlayerSubtitleSelectionEffect(controller, selections.selectedSubtitleKey)
    PlayerProgressEffects(
        controller = controller,
        activity = activity,
        durationMillis = stream.durationSeconds * 1_000L,
        audioOnlyAvailable = !stream.isLive && !stream.isLiveContent,
        pipSourceRect = null,
        onSaveProgress = onSaveProgress,
    )
    PlaybackKeepScreenOnEffect(
        window = activity?.window,
        videoIsPlaying = playbackStatus?.isPlaying == true,
    )
    ShortsEndedEffect(controller, onAdvance)

    if (controller == null || playbackStatus == null || currentMediaId != state.videoUrl) {
        CircularProgressIndicator()
        return
    }
    val toggleDescription = stringResource(R.string.player_play_pause)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = toggleDescription }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
            ) {
                if (controller.isPlaying) controller.pause() else controller.play()
            },
    ) {
        ResilientPlayerSurface(
            player = controller,
            surfaceKey = stream.id,
            resizeMode = resizeMode,
            showNativeSubtitles = false,
            captionStyles = state.userSettings.captionStyles,
            modifier = Modifier.fillMaxSize(),
        )
        PlayerSubtitleOverlay(
            player = controller,
            controlsVisible = false,
            subtitlesVisible = selections.selectedSubtitleKey != null,
            externalSource = externalSubtitle,
            loadExternalCues = loadSubtitleCues,
            captionStyles = state.userSettings.captionStyles,
            modifier = Modifier.fillMaxSize(),
        )
        AnimatedVisibility(
            visible = playbackStatus.isBuffering,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            CircularProgressIndicator()
        }
        if (
            playbackStatus.error == null &&
            !playbackStatus.isPlaying &&
            !playbackStatus.isBuffering
        ) {
            FilledIconButton(
                onClick = controller::play,
                modifier = Modifier.align(Alignment.Center).size(68.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.shorts_play, stream.title),
                    modifier = Modifier.size(38.dp),
                )
            }
        }
        playbackStatus.error?.let {
            FilledIconButton(
                onClick = onRetry,
                modifier = Modifier.align(Alignment.Center).size(68.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = stringResource(R.string.action_retry),
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        SponsorBlockPlaybackFeedback(
            player = controller,
            policy = sponsorBlockPolicy,
            visible = true,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .safeDrawingPadding()
                .padding(top = 64.dp, end = 16.dp),
        )
        ShortsPlaybackOptionsButton(
            onClick = { playbackOptionsVisible = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .safeDrawingPadding()
                .padding(12.dp),
        )
        if (playbackOptionsVisible) {
            PlaybackOptionsSheet(
                player = controller,
                stream = stream,
                selectedCodec = selections.selectedCodec,
                selectedQuality = selections.selectedQuality,
                selectedAudioKey = selections.selectedAudioKey,
                selectedSubtitleKey = selections.selectedSubtitleKey,
                selectedSpeed = selections.selectedSpeed,
                codecSupport = codecSupport,
                resizeMode = resizeMode,
                audioOnlyEnabled = false,
                audioOnlyChanging = false,
                showAudioOnly = false,
                onSelectCodec = selections::selectCodec,
                onSelectQuality = selections::selectQuality,
                onSelectAudio = selections::selectAudio,
                onSelectSubtitle = selections::selectSubtitle,
                onSelectSpeed = selections::selectSpeed,
                onSelectResizeMode = { resizeMode = it },
                onAudioOnlyChange = {},
                onDismiss = { playbackOptionsVisible = false },
            )
        }
    }
}

@Composable
internal fun ShortsPlaybackOptionsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledIconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = stringResource(R.string.player_playback_options),
        )
    }
}

@Composable
private fun ShortsEndedEffect(player: Player?, onAdvance: () -> Unit) {
    val currentOnAdvance by rememberUpdatedState(onAdvance)
    DisposableEffect(player) {
        var advancedMediaId: String? = null
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState != Player.STATE_ENDED) return
                val mediaId = player?.currentMediaItem?.mediaId ?: return
                if (mediaId == advancedMediaId) return
                advancedMediaId = mediaId
                currentOnAdvance()
            }
        }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }
}
