package dev.typetype.android.services

import android.os.Bundle
import androidx.media3.common.MediaItem
import dev.typetype.android.domain.stream.AudioOnlyStream
import dev.typetype.android.domain.stream.StreamRequestScope

internal data class ProviderAudioOnlyRequest(
    val videoUrl: String,
    val requestScope: StreamRequestScope,
    val preferOriginal: Boolean,
    val preferredLocale: String,
)

internal fun MediaItem.providerAudioOnlyRequest(): ProviderAudioOnlyRequest? {
    val extras = requestMetadata.extras ?: return null
    if (!extras.getString(MergedStreamMediaKeys.EXTRA_SABR_REQUEST_KEY).isNullOrBlank()) return null
    if (extras.getBoolean(MergedStreamMediaKeys.EXTRA_IS_LIVE_CONTENT)) return null
    val videoUrl = mediaId.takeIf { it.isNotBlank() } ?: return null
    return ProviderAudioOnlyRequest(
        videoUrl = videoUrl,
        requestScope = extras.streamRequestScope() ?: return null,
        preferOriginal = extras.getBoolean(
            MergedStreamMediaKeys.EXTRA_AUDIO_ONLY_PREFER_ORIGINAL,
        ),
        preferredLocale = extras.getString(
            MergedStreamMediaKeys.EXTRA_AUDIO_ONLY_PREFERRED_LOCALE,
        ).orEmpty(),
    )
}

internal fun MediaItem.isProviderAudioOnly(): Boolean =
    requestMetadata.extras?.getBoolean(
        MergedStreamMediaKeys.EXTRA_AUDIO_ONLY_ACTIVE,
    ) == true && requestMetadata.extras?.getString(
        MergedStreamMediaKeys.EXTRA_SABR_REQUEST_KEY,
    ).isNullOrBlank()

internal fun MediaItem.withProviderAudioOnly(stream: AudioOnlyStream): MediaItem {
    requireNotNull(providerAudioOnlyRequest())
    val extras = Bundle(requestMetadata.extras ?: Bundle()).apply {
        putBoolean(MergedStreamMediaKeys.EXTRA_AUDIO_ONLY_ACTIVE, true)
        putString(MergedStreamMediaKeys.EXTRA_SOURCE_KEY, "audio-only:${stream.url}")
        remove(MergedStreamMediaKeys.EXTRA_AUDIO_URL)
        remove(MergedStreamMediaKeys.EXTRA_AUDIO_MIME_TYPE)
        remove(MergedStreamMediaKeys.EXTRA_VIDEO_MIME_TYPE)
    }
    return buildUpon()
        .setUri(stream.url)
        .setMimeType(stream.mimeType)
        .setSubtitleConfigurations(emptyList())
        .setRequestMetadata(requestMetadata.buildUpon().setExtras(extras).build())
        .build()
}
