package dev.typetype.android.feature.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.typetype.android.feature.player.state.PlayerGestureState

private const val SEEK_HINT_VISIBLE_MS = 650

@Composable
internal fun PlaybackHintOverlay(state: PlayerGestureState) {
    val playing = state.playbackHintPlaying.value
    val pulse = state.playbackHintPulse.longValue
    LaunchedEffect(playing, pulse) {
        if (playing != null) {
            kotlinx.coroutines.delay(SEEK_HINT_VISIBLE_MS.toLong())
            state.playbackHintPlaying.value = null
        }
    }
    if (playing != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.Black.copy(alpha = 0.68f))
                    .padding(18.dp),
            ) {
                Icon(
                    imageVector = if (playing) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
internal fun SpeedBoostBadge(visible: Boolean, factor: Float) {
    if (!visible) return
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(
                text = "${factor}x",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
