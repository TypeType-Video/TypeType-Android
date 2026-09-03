package video.typetype.tv.player

import android.content.Intent
import video.typetype.sdk.core.SubtitleTrack
import video.typetype.sdk.media3.ManifestProtocol

internal const val ACTION_PLAY = "video.typetype.tv.action.PLAY"
internal const val ACTION_STOP = "video.typetype.tv.action.STOP"
internal const val ACTION_SEEK = "video.typetype.tv.action.SEEK"
internal const val ACTION_RETRY = "video.typetype.tv.action.RETRY"
internal const val SESSION_ID = "session_id"
internal const val VIDEO_ID = "video_id"
internal const val VIDEO_URL = "video_url"
internal const val TITLE = "title"
internal const val THUMBNAIL_URL = "thumbnail_url"
internal const val DURATION = "duration"
internal const val VIDEO_ITAG = "video_itag"
internal const val AUDIO_ITAG = "audio_itag"
internal const val AUDIO_TRACK_ID = "audio_track_id"
internal const val GENERATION = "generation"
internal const val START_TIME = "start_time"
internal const val VIDEO_MIME = "video_mime"
internal const val AUDIO_MIME = "audio_mime"
internal const val VIDEO_TRACK_ITAGS = "video_track_itags"
internal const val VIDEO_TRACK_MIMES = "video_track_mimes"
internal const val MANIFEST_URL = "manifest_url"
internal const val MANIFEST_PROTOCOL = "manifest_protocol"
internal const val AUDIO_ONLY_URL = "audio_only_url"
internal const val AUDIO_ONLY_MIME = "audio_only_mime"
internal const val AUDIO_ONLY_KIND = "audio_only_kind"
internal const val IS_LIVE = "is_live"
internal const val SUBTITLE_LANGUAGE = "subtitle_language"
internal const val SUBTITLE_NAME = "subtitle_name"
internal const val SUBTITLE_AUTO = "subtitle_auto"
internal const val SUBTITLE_SOURCE_LANGUAGE = "subtitle_source_language"
internal const val SUBTITLE_TRANSLATION = "subtitle_translation"
internal const val SUBTITLE_NAME_ID = "subtitle_name_id"
internal const val SUBTITLE_ERROR_EXTRA = "subtitle_error"
internal const val PLAYBACK_ERROR_EXTRA = "playback_error"
internal const val SEEK_TIME = "seek_time"
internal const val TRACK_PROGRESS = "track_progress"
internal const val PLAYBACK_SPEED = "playback_speed"
internal const val PLAYBACK_VOLUME = "playback_volume"

internal fun Intent.toPlaybackRequest(): TvPlaybackRequest? {
    val sessionId = getStringExtra(SESSION_ID) ?: return null
    val videoId = getStringExtra(VIDEO_ID) ?: return null
    val videoUrl = getStringExtra(VIDEO_URL) ?: return null
    val title = getStringExtra(TITLE) ?: return null
    val thumbnailUrl = getStringExtra(THUMBNAIL_URL) ?: return null
    val manifestUrl = getStringExtra(MANIFEST_URL)?.takeIf(String::isNotBlank)
    val manifestProtocol = getStringExtra(MANIFEST_PROTOCOL)?.let { value ->
        runCatching { ManifestProtocol.valueOf(value.uppercase()) }.getOrNull()
    }
    val audioOnlyUrl = getStringExtra(AUDIO_ONLY_URL)?.takeIf(String::isNotBlank)
    val audioOnlyMime = getStringExtra(AUDIO_ONLY_MIME)?.takeIf(String::isNotBlank)
    if (manifestUrl != null && manifestProtocol == null) return null
    if (audioOnlyUrl != null && audioOnlyMime == null) return null
    val videoMime = getStringExtra(VIDEO_MIME)
    val audioMime = getStringExtra(AUDIO_MIME)
    val trackItags = getIntArrayExtra(VIDEO_TRACK_ITAGS)?.toList().orEmpty()
    val trackMimes = getStringArrayListExtra(VIDEO_TRACK_MIMES)?.toList().orEmpty()
    val videoTracks = trackItags.zip(trackMimes).mapNotNull { (itag, mime) ->
        itag.takeIf { it > 0 }?.let { TvVideoTrack(it, mime) }
    }
    val videoItag = getIntExtra(VIDEO_ITAG, -1).takeIf { it > 0 }
    val audioItag = getIntExtra(AUDIO_ITAG, -1).takeIf { it > 0 }
    if (manifestUrl == null && audioOnlyUrl == null &&
        (videoMime == null || audioMime == null || videoItag == null || audioItag == null)
    ) return null
    return TvPlaybackRequest(
        sessionId = sessionId,
        videoId = videoId,
        videoUrl = videoUrl,
        title = title,
        thumbnailUrl = thumbnailUrl,
        durationMilliseconds = getLongExtra(DURATION, 0L).takeIf { it > 0L },
        videoItag = videoItag,
        audioItag = audioItag,
        audioTrackId = getStringExtra(AUDIO_TRACK_ID),
        generation = getLongExtra(GENERATION, 0L),
        startTimeMilliseconds = getLongExtra(START_TIME, 0L),
        videoMimeType = videoMime,
        audioMimeType = audioMime,
        videoTracks = videoTracks,
        manifestUrl = manifestUrl,
        manifestProtocol = manifestProtocol,
        audioOnlyUrl = audioOnlyUrl,
        audioOnlyMimeType = audioOnlyMime,
        audioOnlyKind = getStringExtra(AUDIO_ONLY_KIND),
        isLive = getBooleanExtra(IS_LIVE, false),
        trackProgress = getBooleanExtra(TRACK_PROGRESS, true),
        playbackSpeed = getFloatExtra(PLAYBACK_SPEED, 1f).coerceIn(.25f, 4f),
        playbackVolume = getFloatExtra(PLAYBACK_VOLUME, 1f).coerceIn(0f, 1f),
        sponsorBlockPolicy = toSponsorBlockPolicy(),
    )
}

internal fun Intent.toSubtitleTrack(): SubtitleTrack? {
    val language = getStringExtra(SUBTITLE_LANGUAGE) ?: return null
    return SubtitleTrack(
        languageTag = language,
        displayName = getStringExtra(SUBTITLE_NAME),
        isAutoGenerated = getBooleanExtra(SUBTITLE_AUTO, false),
        sourceLanguage = getStringExtra(SUBTITLE_SOURCE_LANGUAGE),
        translationLanguage = getStringExtra(SUBTITLE_TRANSLATION),
        name = getStringExtra(SUBTITLE_NAME_ID),
    )
}
