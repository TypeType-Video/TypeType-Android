package dev.typetype.android.services

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.data.stream.SabrPlaybackRecoveryException
import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import java.io.IOException

internal class SabrPlaybackSessionReplacementRequiredException(
    val session: SabrPlaybackSession,
) : IOException("SABR requires a replacement playback session")

internal class SabrPlaybackRecoveryGate {
    private var activeMediaId: String? = null
    private var recoveryCount = 0
    private var recoveringSessionId: String? = null
    private var stableStartedAtMs: Long? = null
    private var lastProgressAtMs: Long? = null
    private var lastPositionMs: Long? = null
    private val handledSessionIds = mutableSetOf<String>()

    fun transition(mediaId: String?, startsNewSession: Boolean = false) {
        if (mediaId == null || mediaId == activeMediaId && !startsNewSession) return
        activeMediaId = mediaId
        reset()
    }

    fun begin(mediaId: String, sessionId: String): SabrPlaybackRecoveryDecision {
        transition(mediaId)
        if (sessionId == recoveringSessionId) {
            return SabrPlaybackRecoveryDecision.Ignore
        }
        if (sessionId in handledSessionIds) return SabrPlaybackRecoveryDecision.Exhausted
        handledSessionIds += sessionId
        if (recoveryCount >= MAX_SESSION_RECOVERIES) {
            return SabrPlaybackRecoveryDecision.Exhausted
        }
        recoveringSessionId = sessionId
        stableStartedAtMs = null
        lastProgressAtMs = null
        lastPositionMs = null
        return SabrPlaybackRecoveryDecision.Recover
    }

    fun takeAttempt(): Boolean {
        if (recoveryCount >= MAX_SESSION_RECOVERIES) return false
        recoveryCount++
        return true
    }

    fun finish(sessionId: String) {
        if (recoveringSessionId == sessionId) recoveringSessionId = null
    }

    fun observeProgress(mediaId: String?, positionMs: Long, nowMs: Long) {
        if (mediaId.isNullOrBlank() || positionMs < 0L) return
        transition(mediaId)
        val previousTime = lastProgressAtMs
        val previousPosition = lastPositionMs
        if (previousTime == null || previousPosition == null) {
            stableStartedAtMs = nowMs
            lastProgressAtMs = nowMs
            lastPositionMs = positionMs
            return
        }
        val elapsedMs = nowMs - previousTime
        val advancedMs = positionMs - previousPosition
        val continuous = elapsedMs in 1..MAX_PROGRESS_GAP_MS &&
            advancedMs > 0L &&
            advancedMs <= elapsedMs * MAX_PROGRESS_RATE + PROGRESS_TOLERANCE_MS
        stableStartedAtMs = if (continuous) stableStartedAtMs ?: nowMs else nowMs
        lastProgressAtMs = nowMs
        lastPositionMs = positionMs
        if (
            recoveryCount > 0 &&
            stableStartedAtMs?.let { nowMs - it >= STABLE_PLAYBACK_RESET_MS } == true
        ) {
            reset()
        }
    }

    private fun reset() {
        recoveryCount = 0
        recoveringSessionId = null
        stableStartedAtMs = null
        lastProgressAtMs = null
        lastPositionMs = null
        handledSessionIds.clear()
    }
}

internal enum class SabrPlaybackRecoveryDecision {
    Recover,
    Ignore,
    Exhausted,
}

internal fun startsNewSabrPlaybackSession(
    previous: SabrPlaybackBinding?,
    next: SabrPlaybackBinding?,
    continuesCurrentSession: Boolean,
): Boolean = next != null && next != previous && !continuesCurrentSession

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
internal fun Throwable.isRecoverableSabrSessionFailure(): Boolean {
    if (sabrPlaybackRecoveryExhaustedFailure() != null) return false
    if (sabrSessionReplacementFailure() != null) return true
    var current: Throwable? = this
    repeat(MAX_CAUSE_DEPTH) {
        val recovery = current as? SabrPlaybackRecoveryException
        if (recovery?.action in RECOVERABLE_WINDOW_ACTIONS) return true
        val coded = current as? CodedFailure
        if (
            coded?.failureCode == SABR_CONTRACT_FAILURE_CODE ||
            coded?.statusCode?.isRecoverableSabrSessionStatus() == true
        ) {
            return true
        }
        val response = current as? HttpDataSource.InvalidResponseCodeException
        if (
            response?.responseCode?.isRecoverableSabrSessionStatus() == true &&
            response.dataSpec.uri.pathSegments.isSabrPlaybackPayloadPath()
        ) {
            return true
        }
        current = current?.cause?.takeUnless { it === current }
    }
    return false
}

private fun Throwable.sabrPlaybackRecoveryExhaustedFailure():
    SabrPlaybackRecoveryExhaustedException? {
    var current: Throwable? = this
    repeat(MAX_CAUSE_DEPTH) {
        if (current is SabrPlaybackRecoveryExhaustedException) return current
        current = current?.cause?.takeUnless { it === current }
    }
    return null
}

internal fun Throwable.sabrPlaybackRecoveryFailure(): SabrPlaybackRecoveryException? {
    var current: Throwable? = this
    repeat(MAX_CAUSE_DEPTH) {
        if (current is SabrPlaybackRecoveryException) return current
        current = current?.cause?.takeUnless { it === current }
    }
    return null
}

internal fun SabrPlaybackTarget.recoveryTarget(
    failure: SabrPlaybackRecoveryException,
): SabrPlaybackTarget? = when (failure.action) {
    "retry_fresh_session" -> this
    "retry_fresh_session_lower_video_itag" -> failure.retryVideoItags
        .firstOrNull { it in recoveryVideoItags }
        ?.let { videoItag ->
            copy(
                videoItag = videoItag,
                recoveryVideoItags = recoveryVideoItags.filterTo(linkedSetOf()) {
                    it != videoItag
                },
            )
        }
    else -> null
}

internal fun Throwable.sabrSessionReplacementFailure():
    SabrPlaybackSessionReplacementRequiredException? {
    var current: Throwable? = this
    repeat(MAX_CAUSE_DEPTH) {
        if (current is SabrPlaybackSessionReplacementRequiredException) return current
        current = current?.cause?.takeUnless { it === current }
    }
    return null
}

internal fun Int.isRecoverableSabrSessionStatus(): Boolean =
    this in RECOVERABLE_SESSION_STATUS_CODES

private val RECOVERABLE_SESSION_STATUS_CODES = setOf(202, 404, 409, 410)
private val RECOVERABLE_WINDOW_ACTIONS = setOf(
    "retry_fresh_session",
    "retry_fresh_session_lower_video_itag",
)
private const val MAX_SESSION_RECOVERIES = 2
private const val MAX_CAUSE_DEPTH = 8
private const val STABLE_PLAYBACK_RESET_MS = 30_000L
private const val MAX_PROGRESS_GAP_MS = 2_000L
private const val MAX_PROGRESS_RATE = 4L
private const val PROGRESS_TOLERANCE_MS = 500L
