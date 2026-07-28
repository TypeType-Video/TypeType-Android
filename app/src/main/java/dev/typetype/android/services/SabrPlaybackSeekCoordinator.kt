package dev.typetype.android.services

import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackRepository
import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import dev.typetype.android.domain.stream.accept
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal data class SabrPlaybackSeekState(
    val mediaId: String,
    val target: SabrPlaybackTarget,
    val binding: SabrPlaybackBinding,
    val windowEndMs: Long = 0L,
    val durationMs: Long = 0L,
    val endOfStream: Boolean = false,
    val liveActive: Boolean = false,
    val liveSeekableStartMs: Long = 0L,
    val liveSeekableEndMs: Long = 0L,
)

internal class SabrPlaybackSeekCoordinator(
    private val repository: SabrPlaybackRepository,
    private val scope: CoroutineScope,
    private val currentState: () -> SabrPlaybackSeekState?,
    private val apply: (SabrPlaybackSession, SabrPlaybackTarget, Long) -> Unit,
) {
    private var requestId = 0L
    private var seekJob: Job? = null

    fun seek(state: SabrPlaybackSeekState, positionMs: Long): Job {
        return request(state, positionMs) {
            repository.seek(state.target, state.binding, it).map { session ->
                PreparedSabrSession(session, state.target)
            }
        }
    }

    fun switchAudioOnly(
        state: SabrPlaybackSeekState,
        enabled: Boolean,
        positionMs: Long,
        complete: (Result<Unit>) -> Unit,
    ): Job {
        val target = state.target.copy(audioOnly = enabled)
        return request(state, positionMs, complete) {
            repository.seek(target, state.binding, it).map { session ->
                PreparedSabrSession(session, target)
            }
        }
    }

    fun recover(
        state: SabrPlaybackSeekState,
        target: SabrPlaybackTarget,
        positionMs: Long,
        complete: (Result<Unit>) -> Unit = {},
    ): Job {
        return request(state, positionMs, complete) {
            repository.recoverOnce(target, it).map { session ->
                PreparedSabrSession(session, target)
            }
        }
    }

    fun recoverBounded(
        state: SabrPlaybackSeekState,
        target: SabrPlaybackTarget,
        positionMs: Long,
        initialFailure: Throwable,
        takeAttempt: () -> Boolean,
        complete: (Result<Unit>) -> Unit = {},
    ): Job = request(state, positionMs, complete) { requestedPositionMs ->
        var currentTarget = target
        var lastFailure = initialFailure
        while (takeAttempt()) {
            val result = repository.recoverOnce(currentTarget, requestedPositionMs)
            val session = result.getOrNull()
            if (session != null) {
                return@request Result.success(PreparedSabrSession(session, currentTarget))
            }
            lastFailure = result.exceptionOrNull() ?: lastFailure
            val recovery = lastFailure.sabrPlaybackRecoveryFailure()
            if (recovery != null) {
                currentTarget = currentTarget.recoveryTarget(recovery)
                    ?: return@request Result.failure(lastFailure)
            }
        }
        Result.failure(lastFailure)
    }

    private fun request(
        state: SabrPlaybackSeekState,
        positionMs: Long,
        complete: (Result<Unit>) -> Unit = {},
        load: suspend (Long) -> Result<PreparedSabrSession>,
    ): Job {
        seekJob?.cancel()
        val requestedId = ++requestId
        val requestedPositionMs = positionMs.coerceAtLeast(0L)
        return scope.launch {
            val result = load(requestedPositionMs)
            val prepared = result.getOrElse {
                complete(Result.failure(it))
                return@launch
            }
            val acceptedTarget = runCatching {
                prepared.target.accept(prepared.session)
            }.getOrElse {
                complete(Result.failure(it))
                return@launch
            }
            if (requestedId == requestId && currentState() == state) {
                runCatching {
                    apply(prepared.session, acceptedTarget, requestedPositionMs)
                }.fold(
                    onSuccess = { complete(Result.success(Unit)) },
                    onFailure = { complete(Result.failure(it)) },
                )
            } else {
                complete(Result.success(Unit))
            }
        }.also { seekJob = it }
    }

    fun cancel() {
        requestId++
        seekJob?.cancel()
        seekJob = null
    }
}

private data class PreparedSabrSession(
    val session: SabrPlaybackSession,
    val target: SabrPlaybackTarget,
)
