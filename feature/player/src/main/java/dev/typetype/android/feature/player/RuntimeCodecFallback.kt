package dev.typetype.android.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import dev.typetype.android.playback.PlaybackCodecSupport

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
                val failure = codecFailure(error.errorCode, messages)
                if (failure == CodecFailure.None) return
                val format = activeFormat ?: return
                if (codecSupport.rejectVideo(format, lasting = failure == CodecFailure.Lasting)) fallback.value()
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
}

internal enum class CodecFailure { None, ThisSession, Lasting }

internal fun codecFailure(errorCode: Int, messages: List<String>): CodecFailure {
    val mentionsSurface = messages.any { it.contains("surface", ignoreCase = true) }
    return when (errorCode) {
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        -> CodecFailure.Lasting
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
            if (mentionsSurface) CodecFailure.None else CodecFailure.ThisSession
        else -> CodecFailure.None
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
