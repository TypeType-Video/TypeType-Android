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
        return request(state, state.target, positionMs) {
            repository.seek(state.target, state.binding, it)
        }
    }

    fun recover(
        state: SabrPlaybackSeekState,
        target: SabrPlaybackTarget,
        positionMs: Long,
        complete: (Result<Unit>) -> Unit = {},
    ): Job {
        return request(state, target, positionMs, complete) {
            repository.prepare(target, it)
        }
    }

    private fun request(
        state: SabrPlaybackSeekState,
        target: SabrPlaybackTarget,
        positionMs: Long,
        complete: (Result<Unit>) -> Unit = {},
        load: suspend (Long) -> Result<SabrPlaybackSession>,
    ): Job {
        seekJob?.cancel()
        val requestedId = ++requestId
        val requestedPositionMs = positionMs.coerceAtLeast(0L)
        return scope.launch {
            val result = load(requestedPositionMs)
            val session = result.getOrElse {
                complete(Result.failure(it))
                return@launch
            }
            val acceptedTarget = runCatching { target.accept(session) }.getOrElse {
                complete(Result.failure(it))
                return@launch
            }
            if (requestedId == requestId && currentState() == state) {
                runCatching {
                    apply(session, acceptedTarget, requestedPositionMs)
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
