package dev.typetype.android.services

internal class LivePlaybackFollower {
    private var mediaId: String? = null
    private var initialized = false
    private var following = false
    private var nextCatchUpAtMs = 0L

    fun transition(nextMediaId: String?) {
        mediaId = nextMediaId
        initialized = false
        following = false
        nextCatchUpAtMs = 0L
    }

    fun initialize(
        currentMediaId: String?,
        positionMs: Long,
        targetMs: Long?,
    ) {
        if (initialized || currentMediaId != mediaId || targetMs == null) return
        initialized = true
        following = isNearTarget(positionMs, targetMs)
    }

    fun observeSeek(
        currentMediaId: String?,
        positionMs: Long,
        targetMs: Long?,
    ) {
        if (currentMediaId != mediaId) return
        nextCatchUpAtMs = 0L
        following = targetMs != null && isNearTarget(positionMs, targetMs)
    }

    fun nextTarget(
        currentMediaId: String?,
        positionMs: Long,
        targetMs: Long?,
        playing: Boolean,
        busy: Boolean,
        nowMs: Long,
    ): Long? {
        if (
            currentMediaId != mediaId || !following || targetMs == null ||
            !playing || busy || nowMs < nextCatchUpAtMs ||
            targetMs - positionMs <= MAX_TARGET_DRIFT_MS
        ) {
            return null
        }
        nextCatchUpAtMs = nowMs + CATCH_UP_COOLDOWN_MS
        return targetMs
    }

    private fun isNearTarget(positionMs: Long, targetMs: Long): Boolean =
        positionMs >= targetMs - REJOIN_TOLERANCE_MS
}

private const val MAX_TARGET_DRIFT_MS = 20_000L
private const val REJOIN_TOLERANCE_MS = 5_000L
private const val CATCH_UP_COOLDOWN_MS = 15_000L
