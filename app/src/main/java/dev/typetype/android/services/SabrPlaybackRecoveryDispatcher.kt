package dev.typetype.android.services

import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.data.stream.SabrPlaybackRecoveryException
import java.io.IOException

internal class SabrPlaybackRecoveryDispatcher {
    private var listener: Listener? = null

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun request(
        sessionId: String,
        failure: SabrPlaybackRecoveryException,
        complete: (Result<Unit>) -> Unit,
    ) {
        val active = listener
        if (active == null) {
            complete(Result.failure(failure))
        } else {
            active.onRecoveryRequired(sessionId, failure, complete)
        }
    }

    fun interface Listener {
        fun onRecoveryRequired(
            sessionId: String,
            failure: SabrPlaybackRecoveryException,
            complete: (Result<Unit>) -> Unit,
        )
    }
}

internal class SabrPlaybackRecoveryExhaustedException(
    cause: Throwable,
) : IOException("SABR playback recovery was exhausted", cause), CodedFailure {
    override val failureCode: String = "youtube_sabr_recovery_exhausted"
    override val requestId: String? = (cause as? CodedFailure)?.requestId
    override val statusCode: Int? = (cause as? CodedFailure)?.statusCode
}
