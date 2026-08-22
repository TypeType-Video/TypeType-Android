package dev.typetype.android.feature.player.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player

@Composable
internal fun rememberCurrentMediaId(player: Player?): String? {
    var mediaId by remember(player) { mutableStateOf(player?.currentMediaItem?.mediaId) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaId = mediaItem?.mediaId
            }
        }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }
    return mediaId
}
