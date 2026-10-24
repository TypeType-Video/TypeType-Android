package dev.typetype.android.feature.player.components

internal class AutomaticSabrPlaybackRecoveryGate {
    private var mediaId: String? = null
    private var claimed = false

    fun claim(nextMediaId: String?): Boolean {
        if (nextMediaId.isNullOrBlank()) return false
        if (nextMediaId != mediaId) {
            mediaId = nextMediaId
            claimed = false
        }
        if (claimed) return false
        claimed = true
        return true
    }

    fun rearm(activeMediaId: String?) {
        if (activeMediaId == mediaId) claimed = false
    }
}
