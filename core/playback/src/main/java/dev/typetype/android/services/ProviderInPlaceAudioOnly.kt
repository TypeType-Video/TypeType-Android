package dev.typetype.android.services

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player

// A provider item that already carries its audio beside the video, a merged Bilibili source or a
// NicoNico master playlist, switches by turning the video track off and on: the audio keeps playing,
// where asking the Server for an audio-only source restarted playback both ways. The disabled video
// track is the audio-only state the player UI reads.
internal fun MediaItem.canSwitchAudioOnlyInPlace(): Boolean {
    if (providerAudioOnlyRequest() == null) return false
    val extras = requestMetadata.extras
    if (!extras?.getString(MergedStreamMediaKeys.EXTRA_AUDIO_URL).isNullOrBlank()) return true
    return localConfiguration?.uri?.toString()?.startsWith(HLS_MASTER_DATA_PREFIX) == true
}

internal fun Player.isVideoTrackDisabled(): Boolean =
    trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_VIDEO)

internal fun Player.setVideoTrackDisabled(disabled: Boolean) {
    if (isVideoTrackDisabled() == disabled) return
    trackSelectionParameters = trackSelectionParameters.buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, disabled)
        .build()
}

private const val HLS_MASTER_DATA_PREFIX = "data:application/vnd.apple.mpegurl"
