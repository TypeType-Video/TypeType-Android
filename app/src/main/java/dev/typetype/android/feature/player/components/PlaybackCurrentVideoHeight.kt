package dev.typetype.android.feature.player.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Tracks

@Composable
fun rememberCurrentVideoHeight(player: Player): Int? {
    var height by remember(player) { mutableStateOf(player.currentTracks.selectedVideoHeight()) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                height = tracks.selectedVideoHeight()
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    return height
}

private fun Tracks.selectedVideoHeight(): Int? =
    groups
        .asSequence()
        .filter { it.type == C.TRACK_TYPE_VIDEO && it.isSelected }
        .flatMap { group ->
            (0 until group.length)
                .asSequence()
                .filter(group::isTrackSelected)
                .map { index -> group.getTrackFormat(index).height }
        }
        .filter { it > 0 }
        .maxOrNull()
