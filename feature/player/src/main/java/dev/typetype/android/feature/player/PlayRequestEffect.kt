package dev.typetype.android.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.media3.common.Player
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter

// A request to open the video the player already holds starts it, even if it was restored or left paused.
@Composable
internal fun PlayRequestEffect(player: Player?, videoUrl: String, playRequests: Flow<String>) {
    LaunchedEffect(player, videoUrl, playRequests) {
        val current = player ?: return@LaunchedEffect
        playRequests.filter { it == videoUrl }.collect {
            if (current.currentMediaItem?.mediaId == videoUrl) current.playWhenReady = true
        }
    }
}
