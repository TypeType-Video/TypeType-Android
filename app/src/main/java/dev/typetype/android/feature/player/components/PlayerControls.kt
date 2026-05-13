package dev.typetype.android.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
        PlayerCenterControls(
            player = player,
            isFullscreen = isFullscreen,
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
                .then(
                    if (isFullscreen) {
                        Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    } else {
                        Modifier
                    },
                )
                .padding(
                    start = if (isFullscreen) 12.dp else 4.dp,
                    end = if (isFullscreen) 8.dp else 4.dp,
                    bottom = if (isFullscreen) 12.dp else 0.dp,
                ),
        )
    }
}

@Composable
private fun TopScrim(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.68f), Color.Transparent),
                ),
            ),
    )
}

@Composable
private fun BottomScrim(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(152.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                ),
            ),
    )
}

@Composable
private fun BackButton(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    OverlayIconButton(
        onClick = onNavigateBack,
        modifier = modifier.padding(8.dp),
    ) {
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
            OverlayIconButton(onClick = onCycleResizeMode) {
                Icon(
                    imageVector = resizeMode.icon(),
                    contentDescription = stringResource(R.string.player_resize_mode),
                    tint = Color.White,
                )
            }
        }
        OverlayIconButton(onClick = onEnterPip) {
            Icon(
                painter = painterResource(R.drawable.ic_pip),
                contentDescription = stringResource(R.string.player_pip),
                tint = Color.White,
            )
        }
        if (chaptersAvailable) {
            OverlayIconButton(onClick = onOpenChapters) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = stringResource(R.string.player_chapters),
                    tint = Color.White,
                )
            }
        }
        OverlayIconButton(onClick = onOpenOptions) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.player_playback_options),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun OverlayIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .padding(2.dp)
            .size(40.dp)
            .background(Color.Black.copy(alpha = 0.28f), CircleShape),
    ) {
        content()
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
    val backgroundModifier = if (isFullscreen) {
        Modifier.background(Color.Black.copy(alpha = 0.34f), RoundedCornerShape(14.dp))
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .height(if (isFullscreen) 52.dp else 40.dp)
            .then(backgroundModifier)
            .padding(start = if (isFullscreen) 8.dp else 2.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerTimeBar(
            player = player,
            segments = sponsorBlockSegments,
            compact = !isFullscreen,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onToggleFullscreen,
            modifier = Modifier.size(if (isFullscreen) 48.dp else 40.dp),
        ) {
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
