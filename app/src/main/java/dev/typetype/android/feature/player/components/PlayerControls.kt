package dev.typetype.android.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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

@Composable
fun PlayerControls(
    player: Player,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        TopScrim(modifier = Modifier.align(Alignment.TopCenter))
        BottomScrim(modifier = Modifier.align(Alignment.BottomCenter))
        BackButton(
            onNavigateBack = onNavigateBack,
            modifier = Modifier.align(Alignment.TopStart),
        )
        CenterControls(
            player = player,
            modifier = Modifier.align(Alignment.Center),
        )
        PlayerTimeBar(
            player = player,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 12.dp),
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

@OptIn(markerClass = [UnstableApi::class])
@Composable
private fun CenterControls(player: Player, modifier: Modifier = Modifier) {
    val playPauseState = rememberPlayPauseButtonState(player)
    val seekBackState = rememberSeekBackButtonState(player)
    val seekForwardState = rememberSeekForwardButtonState(player)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlButton(
            iconRes = R.drawable.ic_rewind,
            contentDescription = stringResource(R.string.player_rewind),
            enabled = seekBackState.isEnabled,
            onClick = { seekBackState.onClick() },
            sizeDp = 56,
        )
        ControlButton(
            iconRes = if (playPauseState.showPlay) R.drawable.ic_play else R.drawable.ic_pause,
            contentDescription = stringResource(R.string.player_play_pause),
            enabled = playPauseState.isEnabled,
            onClick = { playPauseState.onClick() },
            sizeDp = 72,
        )
        ControlButton(
            iconRes = R.drawable.ic_forward,
            contentDescription = stringResource(R.string.player_forward),
            enabled = seekForwardState.isEnabled,
            onClick = { seekForwardState.onClick() },
            sizeDp = 56,
        )
    }
}

@Composable
private fun ControlButton(
    iconRes: Int,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    sizeDp: Int,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(sizeDp.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = Color.White,
        )
    }
}
