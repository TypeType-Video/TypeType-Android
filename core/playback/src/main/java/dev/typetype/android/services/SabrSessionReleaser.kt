package dev.typetype.android.services

import dev.typetype.android.domain.stream.SabrPlaybackRepository
import dev.typetype.android.domain.stream.SabrPlaybackTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// The server keeps only a couple dozen SABR sessions for every viewer; a session left behind by a video change
// holds a slot for minutes. Releasing it is best effort: an older server or a lost request only means the server
// evicts it later, as before.
internal class SabrSessionReleaser(
    private val repository: SabrPlaybackRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private var held: Held? = null

    fun hold(state: SabrPlaybackSeekState?) {
        val next = state?.let { Held(it.target, it.binding.sessionId) }
        val previous = held
        held = next
        if (previous != null && previous.sessionId != next?.sessionId) release(previous)
    }

    fun releaseAll() {
        held?.let(::release)
        held = null
    }

    private fun release(session: Held) {
        scope.launch { repository.release(session.target, session.sessionId) }
    }

    private data class Held(val target: SabrPlaybackTarget, val sessionId: String)
}
