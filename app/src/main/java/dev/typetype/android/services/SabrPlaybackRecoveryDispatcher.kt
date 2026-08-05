package dev.typetype.android.services

import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.data.stream.SabrPlaybackRecoveryException
import java.io.IOException

internal class SabrPlaybackRecoveryDispatcher {
    private var listener: Listener? = null
    private val pendingRecoveries = mutableMapOf<String, PendingRecovery>()

    fun setListener(listener: Listener?) {
        this.listener = listener
        if (listener != null) return
        val abandoned = pendingRecoveries.values.toList()
        pendingRecoveries.clear()
        abandoned.forEach { pending ->
            pending.completions.forEach { it(Result.failure(pending.failure)) }
        }
    }

    fun request(
        sessionId: String,
        failure: SabrPlaybackRecoveryException,
        complete: (Result<Unit>) -> Unit,
    ) {
        val active = listener
        if (active == null) {
            complete(Result.failure(failure))
            return
        }
        pendingRecoveries[sessionId]?.let {
            it.completions += complete
            return
        }
        pendingRecoveries[sessionId] = PendingRecovery(
            failure = failure,
            completions = mutableListOf(complete),
        )
        active.onRecoveryRequired(sessionId, failure) { result ->
            pendingRecoveries.remove(sessionId)?.completions?.forEach { it(result) }
        }
    }

    fun interface Listener {
        fun onRecoveryRequired(
            sessionId: String,
            failure: SabrPlaybackRecoveryException,
            complete: (Result<Unit>) -> Unit,
        )
    }

    private data class PendingRecovery(
        val failure: SabrPlaybackRecoveryException,
        val completions: MutableList<(Result<Unit>) -> Unit>,
    )
}

internal class SabrPlaybackRecoveryExhaustedException(
    cause: Throwable,
) : IOException("SABR playback recovery was exhausted", cause), CodedFailure {
    override val failureCode: String = "youtube_sabr_recovery_exhausted"
    override val requestId: String? = (cause as? CodedFailure)?.requestId
    override val statusCode: Int? = (cause as? CodedFailure)?.statusCode
}
