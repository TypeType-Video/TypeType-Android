package dev.typetype.android.services

import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackSession
import dev.typetype.android.domain.stream.binding
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SabrPlaybackWindowCache @Inject constructor() {
    private val sessions = LinkedHashMap<Key, SabrPlaybackSession>(MAX_SESSIONS, 0.75f, true)

    @Synchronized
    fun put(session: SabrPlaybackSession) {
        if (session.audioWindow?.segments.isNullOrEmpty()) return
        if (session.videoWindow != null && session.videoWindow.segments.isEmpty()) return
        sessions[session.binding.toKey()] = session
        sessions.keys.removeIf { it.sessionId == session.sessionId && it.generation != session.generation }
        while (sessions.size > MAX_SESSIONS) sessions.remove(sessions.keys.first())
    }

    @Synchronized
    fun take(binding: SabrPlaybackBinding): SabrPlaybackSession? =
        sessions.remove(binding.toKey())

    private fun SabrPlaybackBinding.toKey() = Key(
        sessionId = sessionId,
        generation = generation,
        videoItag = videoItag,
        audioItag = audioItag,
        audioTrackId = audioTrackId,
    )

    private data class Key(
        val sessionId: String,
        val generation: Long,
        val videoItag: Int,
        val audioItag: Int,
        val audioTrackId: String?,
    )

    private companion object {
        const val MAX_SESSIONS = 4
    }
}
