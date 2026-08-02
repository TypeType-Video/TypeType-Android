package dev.typetype.android.services

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import dev.typetype.android.domain.stream.binding
import dev.typetype.android.domain.stream.sourceKey

internal fun MediaItem.sabrPlaybackSeekState(): SabrPlaybackSeekState? {
    val extras = requestMetadata.extras ?: return null
    if (extras.getString(MergedStreamMediaKeys.EXTRA_SABR_REQUEST_KEY).isNullOrBlank()) return null
    val binding = extras.sabrPlaybackBinding() ?: return null
    val target = extras.sabrPlaybackTarget() ?: return null
    if (extras.getString(MergedStreamMediaKeys.EXTRA_SOURCE_KEY) != target.sourceKey) return null
    return SabrPlaybackSeekState(
        mediaId = mediaId.takeIf { it.isNotBlank() } ?: return null,
        target = target,
        binding = binding,
        windowEndMs = extras.getLong(MergedStreamMediaKeys.EXTRA_SABR_WINDOW_END_MS),
        durationMs = extras.getLong(MergedStreamMediaKeys.EXTRA_SABR_DURATION_MS),
        endOfStream = extras.getBoolean(MergedStreamMediaKeys.EXTRA_SABR_END_OF_STREAM),
        liveActive = extras.getBoolean(MergedStreamMediaKeys.EXTRA_SABR_LIVE_ACTIVE),
        liveSeekableStartMs = extras.getLong(
            MergedStreamMediaKeys.EXTRA_SABR_LIVE_SEEKABLE_START_MS,
        ),
        liveSeekableEndMs = extras.getLong(
            MergedStreamMediaKeys.EXTRA_SABR_LIVE_SEEKABLE_END_MS,
        ),
    )
}

internal fun MediaItem.withSabrPlayback(
    session: SabrPlaybackSession,
    target: SabrPlaybackTarget,
): MediaItem {
    require(session.binding.videoItag == target.videoItag)
    require(session.binding.audioItag == target.audioItag)
    require(session.binding.audioTrackId == target.audioTrackId)
    val extras = Bundle(requestMetadata.extras ?: Bundle()).apply {
        putString(MergedStreamMediaKeys.EXTRA_SOURCE_KEY, target.sourceKey)
        putString(MergedStreamMediaKeys.EXTRA_SABR_SESSION_ID, session.sessionId)
        putLong(MergedStreamMediaKeys.EXTRA_SABR_GENERATION, session.generation)
        putInt(MergedStreamMediaKeys.EXTRA_SABR_VIDEO_ITAG, target.videoItag)
        putInt(MergedStreamMediaKeys.EXTRA_SABR_AUDIO_ITAG, target.audioItag)
        if (target.audioTrackId == null) {
            remove(MergedStreamMediaKeys.EXTRA_SABR_AUDIO_TRACK_ID)
        } else {
            putString(MergedStreamMediaKeys.EXTRA_SABR_AUDIO_TRACK_ID, target.audioTrackId)
        }
        putString(MergedStreamMediaKeys.EXTRA_SABR_VIDEO_ID, target.videoId)
        putIntArray(
            MergedStreamMediaKeys.EXTRA_SABR_RECOVERY_VIDEO_ITAGS,
            target.recoveryVideoItags.toIntArray(),
        )
        putBoolean(MergedStreamMediaKeys.EXTRA_SABR_SESSION_CONTINUATION, true)
        putBoolean(MergedStreamMediaKeys.EXTRA_SABR_IS_LIVE, target.isLive)
        putBoolean(MergedStreamMediaKeys.EXTRA_SABR_AUDIO_ONLY, target.audioOnly)
        putLong(MergedStreamMediaKeys.EXTRA_SABR_WINDOW_END_MS, session.windowEndMs)
        putLong(MergedStreamMediaKeys.EXTRA_SABR_DURATION_MS, session.durationMs)
        putBoolean(MergedStreamMediaKeys.EXTRA_SABR_END_OF_STREAM, session.endOfStream)
        putBoolean(MergedStreamMediaKeys.EXTRA_SABR_LIVE_ACTIVE, session.live?.active == true)
        putLong(
            MergedStreamMediaKeys.EXTRA_SABR_LIVE_SEEKABLE_START_MS,
            session.live?.seekableStartMs ?: 0L,
        )
        putLong(
            MergedStreamMediaKeys.EXTRA_SABR_LIVE_SEEKABLE_END_MS,
            session.live?.seekableEndMs ?: 0L,
        )
        putLong(MergedStreamMediaKeys.EXTRA_RESUME_POSITION_MILLIS, session.startTimeMs)
    }
    val updatedRequestMetadata = requestMetadata.buildUpon()
        .setExtras(extras)
        .build()
    return buildUpon()
        .setUri(sabrPlaybackMediaUri(session.sessionId))
        .setRequestMetadata(updatedRequestMetadata)
        .build()
}

internal fun sabrPlaybackMediaUri(sessionId: String): String {
    require(sessionId.isNotBlank())
    return "typetype-sabr://playback/$sessionId"
}

internal fun Player.currentSabrMediaTimeMs(state: SabrPlaybackSeekState): Long? =
    sabrMediaTimeMs(currentPosition, state.liveActive)
