package dev.typetype.android.feature.player.host

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import dev.typetype.android.R
import dev.typetype.android.core.ui.branding.rememberVideoBranding
import dev.typetype.android.feature.player.DevicePlaybackCodecSupport
import dev.typetype.android.feature.player.PlayerViewModel
import dev.typetype.android.feature.player.RECOMMENDED_CODEC_KEY
import dev.typetype.android.feature.player.RuntimeCodecFallbackEffect
import dev.typetype.android.feature.player.bindStreamToController
import dev.typetype.android.feature.player.components.MiniPlayerBar
import dev.typetype.android.feature.player.initialAudioKey
import dev.typetype.android.feature.player.initialQuality
import dev.typetype.android.feature.player.initialSubtitleKey
import dev.typetype.android.feature.player.normalizeDefaultPlaybackSpeed
import dev.typetype.android.feature.player.sleep.PlaybackSleepTimerViewModel
import dev.typetype.android.feature.player.sleep.summary

@Composable
internal fun MiniPlayerRuntime(
    controller: MediaController?,
    onExpand: () -> Unit,
    onSendToBackground: () -> Unit,
    onClose: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
    sleepTimerViewModel: PlaybackSleepTimerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sleepTimer by sleepTimerViewModel.state.collectAsStateWithLifecycle()
    val currentItem by rememberCurrentMediaItem(controller)
    val context = LocalContext.current
    val codecSupport = remember(context.applicationContext) {
        DevicePlaybackCodecSupport(context.applicationContext)
    }
    val stream = state.stream
    val settings = state.userSettings
    var codecFallbackGeneration by remember(stream?.id) { mutableLongStateOf(0L) }

    LaunchedEffect(
        controller,
        currentItem,
        stream,
        state.playbackBindGeneration,
        codecFallbackGeneration,
        state.preferredCodec,
        settings.defaultQuality,
        settings.defaultAudioLanguage,
        settings.subtitlesEnabled,
        settings.defaultSubtitleLanguage,
        settings.preferOriginalLanguage,
    ) {
        val player = controller ?: return@LaunchedEffect
        val loaded = stream ?: return@LaunchedEffect
        if (currentItem != null) return@LaunchedEffect
        bindStreamToController(
            controller = player,
            stream = loaded,
            videoUrl = state.videoUrl,
            startMillis = state.resumeAtMillis,
            selectedQuality = loaded.initialQuality(),
            selectedAudioKey = loaded.initialAudioKey(
                settings.defaultAudioLanguage,
                settings.preferOriginalLanguage,
            ),
            selectedSubtitleKey = loaded.initialSubtitleKey(
                settings.subtitlesEnabled,
                settings.defaultSubtitleLanguage,
            ),
            defaultAudioLanguage = settings.defaultAudioLanguage,
            automaticQualityCap = settings.defaultQuality,
            preferOriginalLanguage = settings.preferOriginalLanguage,
            initialPlayWhenReady = state.initialPlayWhenReady,
            codecSupport = codecSupport,
            prepareSabrPlayback = viewModel.sabrPlayback::prepare,
            selectedCodec = state.preferredCodec,
        )
        player.setPlaybackSpeed(normalizeDefaultPlaybackSpeed(settings.defaultPlaybackSpeed))
    }

    RuntimeCodecFallbackEffect(
        player = controller,
        enabled = state.preferredCodec == RECOMMENDED_CODEC_KEY,
        codecSupport = codecSupport,
        onFallback = { codecFallbackGeneration += 1L },
    )

    val item = currentItem
    if (controller != null && item != null) {
        val branding = rememberVideoBranding(
            sourceUrl = state.videoUrl,
            title = item.mediaMetadata.title?.toString().orEmpty(),
            thumbnailUrl = item.mediaMetadata.artworkUri?.toString().orEmpty(),
            durationSeconds = stream?.durationSeconds ?: 0,
        )
        MiniPlayerBar(
            player = controller,
            title = branding.title,
            subtitle = item.mediaMetadata.artist?.toString().orEmpty(),
            artworkUri = branding.thumbnailUrl,
            onExpand = onExpand,
            onSendToBackground = onSendToBackground,
            onClose = onClose,
            sleepTimerLabel = if (sleepTimer.isActive) {
                stringResource(R.string.sleep_timer_active_accessibility, sleepTimer.summary())
            } else {
                null
            },
        )
    } else {
        PendingMiniPlayer(
            title = stream?.title,
            failed = state.error != null,
            onExpand = onExpand,
            onClose = onClose,
        )
    }
}

@Composable
private fun PendingMiniPlayer(
    title: String?,
    failed: Boolean,
    onExpand: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onExpand)
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!failed) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.size(12.dp))
        }
        Text(
            text = title ?: stringResource(
                if (failed) R.string.mini_player_restore_failed
                else R.string.mini_player_restoring,
            ),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
        )
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.mini_player_close),
            )
        }
    }
}

@Composable
private fun rememberCurrentMediaItem(controller: MediaController?): State<MediaItem?> {
    val current = remember(controller) { mutableStateOf(controller?.currentMediaItem) }
    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                current.value = mediaItem
            }
        }
        controller?.addListener(listener)
        current.value = controller?.currentMediaItem
        onDispose { controller?.removeListener(listener) }
    }
    return current
}
