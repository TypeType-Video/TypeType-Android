package dev.typetype.android.feature.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.typetype.android.R
import kotlinx.coroutines.delay

private const val PIP_STATE_VISIBLE_MILLIS = 1_600L

@Composable
internal fun PipPlaybackStateOverlay(
    visible: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    var showState by remember { mutableStateOf(false) }
    LaunchedEffect(visible, isPlaying) {
        showState = visible
        if (visible) {
            delay(PIP_STATE_VISIBLE_MILLIS)
            showState = false
        }
    }
    AnimatedVisibility(
        visible = visible && showState,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                contentDescription = null,
                tint = Color.White,
            )
            Text(
                text = stringResource(
                    if (isPlaying) R.string.player_state_playing else R.string.player_state_paused,
                ),
                color = Color.White,
            )
        }
    }
}
