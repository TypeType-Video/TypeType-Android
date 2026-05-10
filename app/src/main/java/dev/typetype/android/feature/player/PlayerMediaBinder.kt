package dev.typetype.android.feature.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.session.MediaController
import dev.typetype.android.domain.stream.Stream

internal fun bindStreamToController(
    controller: MediaController,
    stream: Stream,
    videoUrl: String,
    startMillis: Long,
) {
    val (sourceUrl, mimeType) = pickPlayableSource(stream)
    if (sourceUrl == null) return
    val metadata = MediaMetadata.Builder()
        .setTitle(stream.title)
        .setArtist(stream.uploaderName)
        .setArtworkUri(Uri.parse(stream.thumbnailUrl))
        .build()
    val mediaItem = MediaItem.Builder()
        .setUri(sourceUrl)
        .setMediaId(videoUrl)
        .setMediaMetadata(metadata)
        .apply { mimeType?.let { setMimeType(it) } }
        .build()
    val sameMedia = controller.currentMediaItem?.mediaId == videoUrl
    if (!sameMedia) {
        if (startMillis > 0) {
            controller.setMediaItem(mediaItem, startMillis)
        } else {
            controller.setMediaItem(mediaItem)
        }
        controller.prepare()
    }
    controller.playWhenReady = true
}

private fun pickPlayableSource(stream: Stream): Pair<String?, String?> = when {
    !stream.hlsUrl.isNullOrBlank() -> stream.hlsUrl to MimeTypes.APPLICATION_M3U8
    !stream.dashMpdUrl.isNullOrBlank() -> stream.dashMpdUrl to MimeTypes.APPLICATION_MPD
    !stream.progressiveUrl.isNullOrBlank() -> stream.progressiveUrl to MimeTypes.VIDEO_MP4
    else -> null to null
}
