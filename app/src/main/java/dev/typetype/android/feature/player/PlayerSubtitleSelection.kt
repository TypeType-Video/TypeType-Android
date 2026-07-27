package dev.typetype.android.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks

@Composable
internal fun PlayerSubtitleSelectionEffect(
    player: Player?,
    selectedSubtitleKey: String?,
) {
    DisposableEffect(player, selectedSubtitleKey) {
        if (player == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onTracksChanged(tracks: Tracks) {
                    player.selectSubtitleTrack(selectedSubtitleKey, tracks)
                }
            }
            player.addListener(listener)
            player.selectSubtitleTrack(selectedSubtitleKey, player.currentTracks)
            onDispose { player.removeListener(listener) }
        }
    }
}

internal fun Player.selectSubtitleTrack(
    selectedSubtitleKey: String?,
    tracks: Tracks,
) {
    val selection = selectedSubtitleKey?.let(tracks::findSubtitleTrack)
    val offSelection = if (selectedSubtitleKey == null) tracks.findSubtitleOffOverride() else null
    val parameters = trackSelectionParameters.buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, selection == null && offSelection == null)
    selection?.let(parameters::setOverrideForType)
    offSelection?.let(parameters::setOverrideForType)
    val updatedParameters = parameters.build()
    if (updatedParameters != trackSelectionParameters) {
        trackSelectionParameters = updatedParameters
    }
}

private fun Tracks.findSubtitleOffOverride(): TrackSelectionOverride? =
    groups.firstOrNull { it.type == C.TRACK_TYPE_TEXT }
        ?.let { TrackSelectionOverride(it.mediaTrackGroup, emptyList()) }

private fun Tracks.findSubtitleTrack(key: String): TrackSelectionOverride? {
    groups.filter { it.type == C.TRACK_TYPE_TEXT }.forEach { group ->
        repeat(group.length) { trackIndex ->
            if (group.getTrackFormat(trackIndex).id.matchesSubtitleConfigurationId(key)) {
                return TrackSelectionOverride(group.mediaTrackGroup, trackIndex)
            }
        }
    }
    return null
}

private fun String?.matchesSubtitleConfigurationId(key: String): Boolean =
    this == key || this?.endsWith(":$key") == true
