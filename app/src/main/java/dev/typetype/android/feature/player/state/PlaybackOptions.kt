package dev.typetype.android.feature.player.state

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks

data class TrackOption(
    val groupType: Int,
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val format: Format,
    val mediaTrackGroup: TrackGroup,
)

val PLAYBACK_SPEEDS: List<Float> = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

fun Tracks.optionsForType(type: Int): List<TrackOption> = buildList {
    var groupIdx = 0
    groups.forEach { group ->
        if (group.type == type && group.isSupported) {
            repeat(group.length) { trackIdx ->
                if (group.isTrackSupported(trackIdx)) {
                    val format = group.getTrackFormat(trackIdx)
                    add(
                        TrackOption(
                            groupType = type,
                            groupIndex = groupIdx,
                            trackIndex = trackIdx,
                            label = labelFor(type, format),
                            format = format,
                            mediaTrackGroup = group.mediaTrackGroup,
                        ),
                    )
                }
            }
        }
        groupIdx++
    }
}

private fun labelFor(type: Int, format: Format): String = when (type) {
    C.TRACK_TYPE_VIDEO -> when {
        format.height > 0 -> "${format.height}p"
        else -> format.label ?: "Track"
    }
    C.TRACK_TYPE_AUDIO -> {
        val language = format.language?.takeIf { it != "und" } ?: ""
        val label = format.label ?: ""
        listOf(language, label).filter { it.isNotBlank() }.joinToString(" - ").ifBlank { "Default" }
    }
    C.TRACK_TYPE_TEXT -> {
        val language = format.language?.takeIf { it != "und" } ?: ""
        val label = format.label ?: ""
        listOf(language, label).filter { it.isNotBlank() }.joinToString(" - ").ifBlank { "Subtitles" }
    }
    else -> format.label ?: "Track"
}

fun Player.currentSpeedOrDefault(): Float = playbackParameters.speed.takeIf { it > 0f } ?: 1f
