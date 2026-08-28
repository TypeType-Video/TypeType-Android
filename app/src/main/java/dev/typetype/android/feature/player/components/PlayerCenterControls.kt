package dev.typetype.android.feature.player.components

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberSeekBackButtonState
import androidx.media3.ui.compose.state.rememberSeekForwardButtonState
import dev.typetype.android.R

@OptIn(markerClass = [UnstableApi::class])
@Composable
internal fun PlayerCenterControls(
    player: Player,
    isFullscreen: Boolean,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val playPauseState = rememberPlayPauseButtonState(player)
    val seekBackState = rememberSeekBackButtonState(player)
    val seekForwardState = rememberSeekForwardButtonState(player)
    val spacing = when {
        isFullscreen -> 44.dp
        compact -> 18.dp
        else -> 26.dp
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerCenterButton(
            iconRes = R.drawable.ic_rewind,
            contentDescription = stringResource(R.string.player_rewind),
            enabled = seekBackState.isEnabled,
            onClick = { seekBackState.onClick() },
            buttonSize = when {
                isFullscreen -> 62.dp
                compact -> 40.dp
                else -> 50.dp
            },
            iconSize = when {
                isFullscreen -> 32.dp
                compact -> 22.dp
                else -> 26.dp
            },
        )
        PlayerCenterButton(
            iconRes = if (playPauseState.showPlay) R.drawable.ic_play else R.drawable.ic_pause,
            contentDescription = stringResource(
                if (playPauseState.showPlay) R.string.player_play else R.string.player_pause,
            ),
            enabled = playPauseState.isEnabled,
            onClick = { playPauseState.onClick() },
            buttonSize = when {
                isFullscreen -> 74.dp
                compact -> 48.dp
                else -> 62.dp
            },
            iconSize = when {
                isFullscreen -> 48.dp
                compact -> 30.dp
                else -> 38.dp
            },
            prominent = true,
        )
        PlayerCenterButton(
            iconRes = R.drawable.ic_forward,
            contentDescription = stringResource(R.string.player_forward),
            enabled = seekForwardState.isEnabled,
            onClick = { seekForwardState.onClick() },
            buttonSize = when {
                isFullscreen -> 62.dp
                compact -> 40.dp
                else -> 50.dp
            },
            iconSize = when {
                isFullscreen -> 32.dp
                compact -> 22.dp
                else -> 26.dp
            },
        )
    }
}

@Composable
private fun PlayerCenterButton(
    iconRes: Int,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    buttonSize: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    prominent: Boolean = false,
) {
    val alpha = if (prominent) 0.58f else 0.34f
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(buttonSize)
            .background(Color.Black.copy(alpha = alpha), CircleShape),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = Color.White.copy(alpha = if (enabled) 1f else 0.45f),
            modifier = Modifier.size(iconSize),
        )
    }
}
