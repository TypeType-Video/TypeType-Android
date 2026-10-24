package dev.typetype.android.services

internal class ProviderAudioOnlyRenewalGate(
    private val minimumIntervalMs: Long = MINIMUM_RENEWAL_INTERVAL_MS,
) {
    private var mediaId: String? = null
    private var inFlight = false
    private var lastAttemptAtMs: Long? = null

    fun transition(nextMediaId: String?) {
        if (nextMediaId == mediaId) return
        mediaId = nextMediaId
        inFlight = false
        lastAttemptAtMs = null
    }

    fun begin(expectedMediaId: String, nowMs: Long): Boolean {
        transition(expectedMediaId)
        if (inFlight) return false
        val previous = lastAttemptAtMs
        if (previous != null && nowMs - previous < minimumIntervalMs) return false
        inFlight = true
        lastAttemptAtMs = nowMs
        return true
    }

    fun finish() {
        inFlight = false
    }

    private companion object {
        const val MINIMUM_RENEWAL_INTERVAL_MS = 60_000L
    }
}
