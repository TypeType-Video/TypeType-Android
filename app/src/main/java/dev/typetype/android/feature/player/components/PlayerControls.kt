package dev.typetype.android.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberSeekBackButtonState
import androidx.media3.ui.compose.state.rememberSeekForwardButtonState
import dev.typetype.android.R
import dev.typetype.android.domain.stream.SponsorBlockSegment
import dev.typetype.android.feature.player.state.ResizeMode

@Composable
fun PlayerControls(
    player: Player,
    onNavigateBack: () -> Unit,
    onOpenOptions: () -> Unit = {},
    onOpenChapters: () -> Unit = {},
    onEnterPip: () -> Unit = {},
    onToggleFullscreen: () -> Unit = {},
    onCycleResizeMode: () -> Unit = {},
    resizeMode: ResizeMode = ResizeMode.Fit,
    isFullscreen: Boolean = false,
    chaptersAvailable: Boolean = false,
    sponsorBlockSegments: List<SponsorBlockSegment> = emptyList(),
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        TopScrim(modifier = Modifier.align(Alignment.TopCenter))
        BottomScrim(modifier = Modifier.align(Alignment.BottomCenter))
        BackButton(
            onNavigateBack = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars),
        )
        TopActions(
            onOpenChapters = onOpenChapters,
            onOpenOptions = onOpenOptions,
            onEnterPip = onEnterPip,
            onCycleResizeMode = onCycleResizeMode,
            resizeMode = resizeMode,
            isFullscreen = isFullscreen,
            chaptersAvailable = chaptersAvailable,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars),
        )
        CenterControls(
            player = player,
            modifier = Modifier.align(Alignment.Center),
        )
        BottomBar(
            player = player,
            sponsorBlockSegments = sponsorBlockSegments,
            isFullscreen = isFullscreen,
            onToggleFullscreen = onToggleFullscreen,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun TopScrim(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent),
                ),
            ),
    )
}

@Composable
private fun BottomScrim(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                ),
            ),
    )
}

@Composable
private fun BackButton(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onNavigateBack, modifier = modifier.padding(8.dp)) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.player_back),
            tint = Color.White,
        )
    }
}

@Composable
private fun TopActions(
    onOpenChapters: () -> Unit,
    onOpenOptions: () -> Unit,
    onEnterPip: () -> Unit,
    onCycleResizeMode: () -> Unit,
    resizeMode: ResizeMode,
    isFullscreen: Boolean,
    chaptersAvailable: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.padding(8.dp)) {
        if (isFullscreen) {
            IconButton(onClick = onCycleResizeMode) {
                Icon(
                    imageVector = resizeMode.icon(),
                    contentDescription = stringResource(R.string.player_resize_mode),
                    tint = Color.White,
                )
            }
        }
        IconButton(onClick = onEnterPip) {
            Icon(
                painter = painterResource(R.drawable.ic_pip),
                contentDescription = stringResource(R.string.player_pip),
                tint = Color.White,
            )
        }
        if (chaptersAvailable) {
            IconButton(onClick = onOpenChapters) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = stringResource(R.string.player_chapters),
                    tint = Color.White,
                )
            }
        }
        IconButton(onClick = onOpenOptions) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.player_playback_options),
                tint = Color.White,
            )
        }
    }
}

private fun ResizeMode.icon(): ImageVector = when (this) {
    ResizeMode.Fit -> Icons.Filled.FitScreen
    ResizeMode.Crop -> Icons.Filled.Crop
    ResizeMode.Stretch -> Icons.Filled.AspectRatio
}

@Composable
private fun BottomBar(
    player: Player,
    sponsorBlockSegments: List<SponsorBlockSegment>,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerTimeBar(
            player = player,
            segments = sponsorBlockSegments,
            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
        )
        IconButton(onClick = onToggleFullscreen) {
            Icon(
                painter = painterResource(
                    if (isFullscreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen,
                ),
                contentDescription = stringResource(R.string.player_fullscreen),
                tint = Color.White,
            )
        }
    }
}

@OptIn(markerClass = [UnstableApi::class])
@Composable
private fun CenterControls(player: Player, modifier: Modifier = Modifier) {
    val playPauseState = rememberPlayPauseButtonState(player)
    val seekBackState = rememberSeekBackButtonState(player)
    val seekForwardState = rememberSeekForwardButtonState(player)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlButton(
            iconRes = R.drawable.ic_rewind,
            contentDescription = stringResource(R.string.player_rewind),
            enabled = seekBackState.isEnabled,
            onClick = { seekBackState.onClick() },
            buttonDp = 64,
            iconDp = 36,
        )
        ControlButton(
            iconRes = if (playPauseState.showPlay) R.drawable.ic_play else R.drawable.ic_pause,
            contentDescription = stringResource(R.string.player_play_pause),
            enabled = playPauseState.isEnabled,
            onClick = { playPauseState.onClick() },
            buttonDp = 80,
            iconDp = 56,
            withScrim = true,
        )
        ControlButton(
            iconRes = R.drawable.ic_forward,
            contentDescription = stringResource(R.string.player_forward),
            enabled = seekForwardState.isEnabled,
            onClick = { seekForwardState.onClick() },
            buttonDp = 64,
            iconDp = 36,
        )
    }
}

@Composable
private fun ControlButton(
    iconRes: Int,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    buttonDp: Int,
    iconDp: Int,
    withScrim: Boolean = false,
) {
    val baseModifier = Modifier.size(buttonDp.dp)
    val modifier = if (withScrim) {
        baseModifier.background(Color.Black.copy(alpha = 0.45f), CircleShape)
    } else {
        baseModifier
    }
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(iconDp.dp),
        )
    }
}
