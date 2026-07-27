package dev.typetype.android.services

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
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

    fun transition(mediaId: String?, startsNewSession: Boolean = false) {
        if (mediaId == null || mediaId == activeMediaId && !startsNewSession) return
        activeMediaId = mediaId
        recoveryCount = 0
    }

    fun acquire(mediaId: String): Boolean {
        transition(mediaId)
        if (recoveryCount >= MAX_SESSION_RECOVERIES) return false
        recoveryCount++
        return true
    }
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

private val RECOVERABLE_SESSION_STATUS_CODES = setOf(202, 403, 404, 409, 410)
private val RECOVERABLE_WINDOW_ACTIONS = setOf(
    "retry_fresh_session",
    "retry_fresh_session_lower_video_itag",
)
private const val MAX_SESSION_RECOVERIES = 2
private const val MAX_CAUSE_DEPTH = 8
