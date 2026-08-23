package dev.typetype.android.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks

@Composable
internal fun RuntimeCodecFallbackEffect(
    player: Player?,
    enabled: Boolean,
    codecSupport: PlaybackCodecSupport,
    onFallback: () -> Unit,
) {
    val fallback = rememberUpdatedState(onFallback)
    DisposableEffect(player, enabled, codecSupport) {
        if (player == null || !enabled) return@DisposableEffect onDispose {}
        var activeFormat = player.currentTracks.selectedVideoFormat()
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                activeFormat = tracks.selectedVideoFormat() ?: activeFormat
            }

            override fun onPlayerError(error: PlaybackException) {
                val messages = generateSequence<Throwable>(error) { it.cause }
                    .mapNotNull(Throwable::message)
                    .toList()
                if (!isCodecCapabilityFailure(error.errorCode, messages)) return
                if (activeFormat?.let(codecSupport::rejectVideo) == true) fallback.value()
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
}

internal fun isCodecCapabilityFailure(errorCode: Int, messages: List<String>): Boolean {
    val mentionsSurface = messages.any { it.contains("surface", ignoreCase = true) }
    return when (errorCode) {
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        -> true
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> !mentionsSurface
        else -> false
    }
}

private fun Tracks.selectedVideoFormat(): Format? = groups
    .asSequence()
    .filter { it.type == C.TRACK_TYPE_VIDEO && it.isSelected }
    .flatMap { group ->
        (0 until group.length).asSequence()
            .filter(group::isTrackSelected)
            .map(group::getTrackFormat)
    }
    .maxByOrNull { it.height }
