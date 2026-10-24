package dev.typetype.android.services

import androidx.media3.common.MediaItem
import androidx.media3.common.Player

internal class AudioOnlyDefaultPolicy : Player.Listener {
    private var currentMediaId: String? = null
    private var hasManualChoice = false

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        mediaChanged(mediaItem?.mediaId)
    }

    fun shouldApplyDefault(mediaId: String?): Boolean {
        mediaChanged(mediaId)
        return !hasManualChoice
    }

    fun recordManualChoice(mediaId: String?) {
        mediaChanged(mediaId)
        hasManualChoice = true
    }

    internal fun mediaChanged(mediaId: String?) {
        if (mediaId == currentMediaId) return
        currentMediaId = mediaId
        hasManualChoice = false
    }
}
