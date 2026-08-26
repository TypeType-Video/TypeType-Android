@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

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
import androidx.media3.common.VideoSize
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.feature.player.codecDisplayName
import dev.typetype.android.feature.player.codecSelectionKey
import dev.typetype.android.services.MergedStreamMediaKeys

internal data class PlaybackActiveVideoFormat(
    val height: Int,
    val codec: String,
    val framesPerSecond: Int?,
    val hdr: Boolean,
) {
    val qualityLabel: String
        get() = listOfNotNull(
            "${height}p",
            framesPerSecond?.let { "$it fps" },
            "HDR".takeIf { hdr },
        ).joinToString(" · ")
}

@Composable
internal fun rememberActiveVideoFormat(
    player: Player,
    stream: Stream? = null,
): PlaybackActiveVideoFormat? {
    var format by remember(player, stream?.id) {
        mutableStateOf(
            player.currentTracks.selectedVideoFormat(player.videoSize)
                ?: player.selectedSabrSourceFormat(stream),
        )
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                format = tracks.selectedVideoFormat(player.videoSize)
                    ?: player.selectedSabrSourceFormat(stream)
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                format = player.currentTracks.selectedVideoFormat(videoSize)
                    ?: player.selectedSabrSourceFormat(stream)
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    return format
}

private fun Tracks.selectedVideoFormat(videoSize: VideoSize): PlaybackActiveVideoFormat? {
    val formats = groups
        .asSequence()
        .filter { it.type == C.TRACK_TYPE_VIDEO && it.isSelected }
        .flatMap { group ->
            (0 until group.length)
                .asSequence()
                .filter(group::isTrackSelected)
                .map(group::getTrackFormat)
        }
        .filter { it.height > 0 }
        .toList()
    val format = formats
        .filter { videoSize.height <= 0 || it.height == videoSize.height }
        .maxByOrNull { it.bitrate }
        ?: formats.maxByOrNull { it.height }
        ?: return null
    return PlaybackActiveVideoFormat(
        height = videoSize.height.takeIf { it > 0 } ?: format.height,
        codec = format.codecs.orEmpty().codecSelectionKey().codecDisplayName(),
        framesPerSecond = format.frameRate.takeIf { it > 0f }?.toInt(),
        hdr = format.colorInfo?.colorTransfer in HDR_TRANSFERS,
    )
}

private fun Player.selectedSabrSourceFormat(
    stream: Stream?,
): PlaybackActiveVideoFormat? {
    val videoItag = currentMediaItem?.requestMetadata?.extras
        ?.getInt(MergedStreamMediaKeys.EXTRA_SABR_VIDEO_ITAG)
        ?.takeIf { it > 0 } ?: return null
    val source = stream?.sabrVideoStreams?.firstOrNull { it.itag == videoItag } ?: return null
    return PlaybackActiveVideoFormat(
        height = source.height,
        codec = source.codec.orEmpty().codecSelectionKey().codecDisplayName(),
        framesPerSecond = source.fps.takeIf { it > 0 },
        hdr = false,
    )
}

private val HDR_TRANSFERS = setOf(C.COLOR_TRANSFER_ST2084, C.COLOR_TRANSFER_HLG)
