package dev.typetype.android.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import dev.typetype.android.R
import dev.typetype.android.domain.stream.SponsorBlockSegment
import dev.typetype.android.feature.player.state.ResizeMode

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun PlayerControls(
    player: Player,
    title: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenOptions: () -> Unit = {},
    onOpenChapters: () -> Unit = {},
    onEnterPip: () -> Unit = {},
    onToggleFullscreen: () -> Unit = {},
    onCycleResizeMode: () -> Unit = {},
    resizeMode: ResizeMode = ResizeMode.Fit,
    isFullscreen: Boolean = false,
    isPipAvailable: Boolean = false,
    chaptersAvailable: Boolean = false,
    sponsorBlockSegments: List<SponsorBlockSegment> = emptyList(),
    seekPreviewPositionMs: Long? = null,
) {
    BoxWithConstraints(modifier = modifier) {
        val compactControls = !isFullscreen && maxHeight < COMPACT_CONTROLS_HEIGHT
        TopScrim(
            compact = compactControls,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        BottomScrim(
            compact = compactControls,
            isFullscreen = isFullscreen,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        PlayerTopBar(
            title = title,
            onNavigateBack = onNavigateBack,
            onOpenChapters = onOpenChapters,
            onOpenOptions = onOpenOptions,
            onEnterPip = onEnterPip,
            onCycleResizeMode = onCycleResizeMode,
            resizeMode = resizeMode,
            isFullscreen = isFullscreen,
            isPipAvailable = isPipAvailable,
            chaptersAvailable = chaptersAvailable,
            compact = compactControls,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .testTag(PLAYER_TOP_CONTROLS_TAG)
                .then(
                    if (isFullscreen) {
                        Modifier.windowInsetsPadding(WindowInsets.statusBars)
                    } else {
                        Modifier
                    },
                ),
        )
        PlayerCenterControls(
            player = player,
            isFullscreen = isFullscreen,
            compact = compactControls,
            modifier = Modifier.align(Alignment.Center).testTag(PLAYER_CENTER_CONTROLS_TAG),
        )
        BottomBar(
            player = player,
            sponsorBlockSegments = sponsorBlockSegments,
            seekPreviewPositionMs = seekPreviewPositionMs,
            isFullscreen = isFullscreen,
            compact = compactControls,
            onToggleFullscreen = onToggleFullscreen,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .testTag(PLAYER_BOTTOM_CONTROLS_TAG)
                .then(
                    if (isFullscreen) {
                        Modifier
                    } else {
                        Modifier.windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
                    },
                )
                .padding(
                    start = if (isFullscreen) 12.dp else 4.dp,
                    end = if (isFullscreen) 8.dp else 4.dp,
                    bottom = if (isFullscreen) 6.dp else 0.dp,
                ),
        )
    }
}

@Composable
private fun TopScrim(compact: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (compact) 72.dp else 112.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.68f), Color.Transparent),
                ),
            ),
    )
}

@Composable
private fun BottomScrim(
    compact: Boolean,
    isFullscreen: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(
                when {
                    isFullscreen -> 116.dp
                    compact -> 88.dp
                    else -> 152.dp
                },
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                ),
            ),
    )
}

@Composable
private fun BottomBar(
    player: Player,
    sponsorBlockSegments: List<SponsorBlockSegment>,
    seekPreviewPositionMs: Long?,
    isFullscreen: Boolean,
    compact: Boolean,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(
                when {
                    isFullscreen -> 52.dp
                    compact -> 36.dp
                    else -> 40.dp
                },
            )
            .padding(start = if (isFullscreen) 8.dp else 2.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerTimeBar(
            player = player,
            segments = sponsorBlockSegments,
            previewPositionMs = seekPreviewPositionMs,
            compact = !isFullscreen,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onToggleFullscreen,
            modifier = Modifier.size(
                when {
                    isFullscreen -> 48.dp
                    compact -> 36.dp
                    else -> 40.dp
                },
            ),
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

internal const val PLAYER_TOP_CONTROLS_TAG = "player_top_controls"
internal const val PLAYER_CENTER_CONTROLS_TAG = "player_center_controls"
internal const val PLAYER_BOTTOM_CONTROLS_TAG = "player_bottom_controls"
private val COMPACT_CONTROLS_HEIGHT = 180.dp
