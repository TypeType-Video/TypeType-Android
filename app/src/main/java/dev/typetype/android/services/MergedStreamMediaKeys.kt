package dev.typetype.android.services

import android.os.Bundle
import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import dev.typetype.android.domain.stream.StreamRequestScope

object MergedStreamMediaKeys {
    const val EXTRA_SOURCE_KEY = "typetype.source_key"
    const val EXTRA_SABR_REQUEST_KEY = "typetype.sabr_request_key"
    const val EXTRA_SABR_SESSION_ID = "typetype.sabr_session_id"
    const val EXTRA_SABR_GENERATION = "typetype.sabr_generation"
    const val EXTRA_SABR_VIDEO_ITAG = "typetype.sabr_video_itag"
    const val EXTRA_SABR_AUDIO_ITAG = "typetype.sabr_audio_itag"
    const val EXTRA_SABR_AUDIO_TRACK_ID = "typetype.sabr_audio_track_id"
    const val EXTRA_SABR_VIDEO_ID = "typetype.sabr_video_id"
    const val EXTRA_SABR_RECOVERY_VIDEO_ITAGS = "typetype.sabr_recovery_video_itags"
    const val EXTRA_SABR_SESSION_CONTINUATION = "typetype.sabr_session_continuation"
    const val EXTRA_SABR_IS_LIVE = "typetype.sabr_is_live"
    const val EXTRA_SABR_AUDIO_ONLY = "typetype.sabr_audio_only"
    const val EXTRA_AUDIO_ONLY_ACTIVE = "typetype.audio_only_active"
    const val EXTRA_AUDIO_ONLY_PREFER_ORIGINAL = "typetype.audio_only_prefer_original"
    const val EXTRA_AUDIO_ONLY_PREFERRED_LOCALE = "typetype.audio_only_preferred_locale"
    const val EXTRA_SABR_WINDOW_END_MS = "typetype.sabr_window_end_ms"
    const val EXTRA_SABR_DURATION_MS = "typetype.sabr_duration_ms"
    const val EXTRA_SABR_END_OF_STREAM = "typetype.sabr_end_of_stream"
    const val EXTRA_SABR_LIVE_ACTIVE = "typetype.sabr_live_active"
    const val EXTRA_SABR_LIVE_SEEKABLE_START_MS = "typetype.sabr_live_seekable_start_ms"
    const val EXTRA_SABR_LIVE_SEEKABLE_END_MS = "typetype.sabr_live_seekable_end_ms"
    const val EXTRA_AUDIO_URL = "typetype.audio_url"
    const val EXTRA_AUDIO_MIME_TYPE = "typetype.audio_mime_type"
    const val EXTRA_VIDEO_MIME_TYPE = "typetype.video_mime_type"
    const val EXTRA_SERVER_ID = "typetype.server_id"
    const val EXTRA_ACCOUNT_ID = "typetype.account_id"
    const val EXTRA_SERVER_BASE_URL = "typetype.server_base_url"
    const val EXTRA_RESUME_POSITION_MILLIS = "typetype.resume_position_millis"
    const val EXTRA_IS_LIVE_CONTENT = "typetype.is_live_content"
    const val EXTRA_VIDEO_TITLE = "typetype.video_title"
    const val EXTRA_VIDEO_THUMBNAIL = "typetype.video_thumbnail"
    const val EXTRA_VIDEO_DURATION_SECONDS = "typetype.video_duration_seconds"
    const val EXTRA_CHANNEL_NAME = "typetype.channel_name"
    const val EXTRA_CHANNEL_URL = "typetype.channel_url"
    const val EXTRA_CHANNEL_AVATAR = "typetype.channel_avatar"
}

internal fun Bundle.streamRequestScope(): StreamRequestScope? {
    val serverId = getString(MergedStreamMediaKeys.EXTRA_SERVER_ID) ?: return null
    val accountId = getString(MergedStreamMediaKeys.EXTRA_ACCOUNT_ID) ?: return null
    val baseUrl = getString(MergedStreamMediaKeys.EXTRA_SERVER_BASE_URL) ?: return null
    return StreamRequestScope(serverId, accountId, baseUrl).takeIf {
        it.serverId.isNotBlank() && it.accountId.isNotBlank() && it.baseUrl.isNotBlank()
    }
}

internal fun Bundle.resumePositionMillis(): Long? =
    takeIf { containsKey(MergedStreamMediaKeys.EXTRA_RESUME_POSITION_MILLIS) }
        ?.getLong(MergedStreamMediaKeys.EXTRA_RESUME_POSITION_MILLIS)
        ?.takeIf { it >= 0L }

internal fun Bundle.sabrPlaybackBinding(): SabrPlaybackBinding? {
    val requiredKeys = listOf(
        MergedStreamMediaKeys.EXTRA_SABR_SESSION_ID,
        MergedStreamMediaKeys.EXTRA_SABR_GENERATION,
        MergedStreamMediaKeys.EXTRA_SABR_VIDEO_ITAG,
        MergedStreamMediaKeys.EXTRA_SABR_AUDIO_ITAG,
    )
    if (requiredKeys.any { !containsKey(it) }) return null
    val binding = SabrPlaybackBinding(
        sessionId = getString(MergedStreamMediaKeys.EXTRA_SABR_SESSION_ID).orEmpty(),
        generation = getLong(MergedStreamMediaKeys.EXTRA_SABR_GENERATION),
        videoItag = getInt(MergedStreamMediaKeys.EXTRA_SABR_VIDEO_ITAG),
        audioItag = getInt(MergedStreamMediaKeys.EXTRA_SABR_AUDIO_ITAG),
        audioTrackId = getString(MergedStreamMediaKeys.EXTRA_SABR_AUDIO_TRACK_ID),
    )
    return binding.takeIf {
        it.sessionId.isNotBlank() && it.generation >= 0L &&
            it.videoItag > 0 && it.audioItag > 0 && it.videoItag != it.audioItag
    }
}

internal fun Bundle.sabrPlaybackTarget(): SabrPlaybackTarget? {
    val scope = streamRequestScope() ?: return null
    val binding = sabrPlaybackBinding() ?: return null
    val videoId = getString(MergedStreamMediaKeys.EXTRA_SABR_VIDEO_ID)
        ?.takeIf { it.isNotBlank() } ?: return null
    val recoveryVideoItags = (
        getIntArray(MergedStreamMediaKeys.EXTRA_SABR_RECOVERY_VIDEO_ITAGS) ?: intArrayOf()
    )
        .filterTo(linkedSetOf()) { it > 0 && it != binding.videoItag && it != binding.audioItag }
    return SabrPlaybackTarget(
        videoId = videoId,
        requestScope = scope,
        videoItag = binding.videoItag,
        audioItag = binding.audioItag,
        audioTrackId = binding.audioTrackId,
        recoveryVideoItags = recoveryVideoItags,
        isLive = getBoolean(MergedStreamMediaKeys.EXTRA_SABR_IS_LIVE),
        audioOnly = getBoolean(MergedStreamMediaKeys.EXTRA_SABR_AUDIO_ONLY),
    )
}
