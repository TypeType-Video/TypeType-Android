package dev.typetype.android.feature.player

import android.os.Bundle
import androidx.media3.common.MediaItem
import dev.typetype.android.domain.stream.StreamRequestScope
import dev.typetype.android.domain.stream.Stream
import dev.typetype.android.services.MergedStreamMediaKeys

internal fun PlayableSource.toRequestMetadata(
    scope: StreamRequestScope?,
    resumePositionMillis: Long = 0L,
    isLiveContent: Boolean = false,
    stream: Stream? = null,
): MediaItem.RequestMetadata {
    val audio = audioUrl?.takeIf { it.isNotBlank() }
    val effectiveScope = sabrTarget?.requestScope ?: scope
    val extras = Bundle().apply {
        putString(MergedStreamMediaKeys.EXTRA_SOURCE_KEY, sourceKey)
        sabrRequestKey?.let { putString(MergedStreamMediaKeys.EXTRA_SABR_REQUEST_KEY, it) }
        sabrBinding?.let {
            putString(MergedStreamMediaKeys.EXTRA_SABR_SESSION_ID, it.sessionId)
            putLong(MergedStreamMediaKeys.EXTRA_SABR_GENERATION, it.generation)
            putInt(MergedStreamMediaKeys.EXTRA_SABR_VIDEO_ITAG, it.videoItag)
            putInt(MergedStreamMediaKeys.EXTRA_SABR_AUDIO_ITAG, it.audioItag)
            it.audioTrackId?.let { trackId ->
                putString(MergedStreamMediaKeys.EXTRA_SABR_AUDIO_TRACK_ID, trackId)
            }
        }
        sabrTarget?.let {
            putString(MergedStreamMediaKeys.EXTRA_SABR_VIDEO_ID, it.videoId)
            putBoolean(MergedStreamMediaKeys.EXTRA_SABR_IS_LIVE, it.isLive)
            putBoolean(MergedStreamMediaKeys.EXTRA_SABR_AUDIO_ONLY, it.audioOnly)
            putIntArray(
                MergedStreamMediaKeys.EXTRA_SABR_RECOVERY_VIDEO_ITAGS,
                it.recoveryVideoItags.toIntArray(),
            )
        }
        sabrSession?.let {
            putLong(MergedStreamMediaKeys.EXTRA_SABR_WINDOW_END_MS, it.windowEndMs)
            putLong(MergedStreamMediaKeys.EXTRA_SABR_DURATION_MS, it.durationMs)
            putBoolean(MergedStreamMediaKeys.EXTRA_SABR_END_OF_STREAM, it.endOfStream)
            putBoolean(MergedStreamMediaKeys.EXTRA_SABR_LIVE_ACTIVE, it.live?.active == true)
            putLong(
                MergedStreamMediaKeys.EXTRA_SABR_LIVE_SEEKABLE_START_MS,
                it.live?.seekableStartMs ?: 0L,
            )
            putLong(
                MergedStreamMediaKeys.EXTRA_SABR_LIVE_SEEKABLE_END_MS,
                it.live?.seekableEndMs ?: 0L,
            )
        }
        audio?.let { putString(MergedStreamMediaKeys.EXTRA_AUDIO_URL, it) }
        audioMimeType?.let { putString(MergedStreamMediaKeys.EXTRA_AUDIO_MIME_TYPE, it) }
        mimeType?.let { putString(MergedStreamMediaKeys.EXTRA_VIDEO_MIME_TYPE, it) }
        effectiveScope?.let {
            putString(MergedStreamMediaKeys.EXTRA_SERVER_ID, it.serverId)
            putString(MergedStreamMediaKeys.EXTRA_ACCOUNT_ID, it.accountId)
            putString(MergedStreamMediaKeys.EXTRA_SERVER_BASE_URL, it.baseUrl)
        }
        putLong(
            MergedStreamMediaKeys.EXTRA_RESUME_POSITION_MILLIS,
            (sabrSession?.startTimeMs ?: resumePositionMillis).coerceAtLeast(0L),
        )
        putBoolean(MergedStreamMediaKeys.EXTRA_IS_LIVE_CONTENT, isLiveContent)
        stream?.let {
            putString(MergedStreamMediaKeys.EXTRA_VIDEO_TITLE, it.title)
            putString(MergedStreamMediaKeys.EXTRA_VIDEO_THUMBNAIL, it.thumbnailUrl)
            putLong(MergedStreamMediaKeys.EXTRA_VIDEO_DURATION_SECONDS, it.durationSeconds)
            putString(MergedStreamMediaKeys.EXTRA_CHANNEL_NAME, it.uploaderName)
            putString(MergedStreamMediaKeys.EXTRA_CHANNEL_URL, it.uploaderUrl)
            putString(MergedStreamMediaKeys.EXTRA_CHANNEL_AVATAR, it.uploaderAvatarUrl)
        }
    }
    return MediaItem.RequestMetadata.Builder()
        .setExtras(extras)
        .build()
}
