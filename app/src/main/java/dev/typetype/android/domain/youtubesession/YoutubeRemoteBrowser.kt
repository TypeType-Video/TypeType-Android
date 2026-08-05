package dev.typetype.android.domain.youtubesession

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class YoutubeRemoteBrowserPhase {
    Idle,
    Connecting,
    Opening,
    AwaitingLogin,
    CapturingSession,
    Connected,
    Closed,
    Error,
}

data class YoutubeRemoteBrowserState(
    val phase: YoutubeRemoteBrowserPhase = YoutubeRemoteBrowserPhase.Idle,
    val errorMessage: String? = null,
)

sealed interface YoutubeRemoteBrowserInput {
    data class Resize(val width: Int, val height: Int) : YoutubeRemoteBrowserInput

    data class Pointer(
        val event: PointerEvent,
        val x: Int,
        val y: Int,
    ) : YoutubeRemoteBrowserInput

    data class Wheel(val deltaX: Float, val deltaY: Float) : YoutubeRemoteBrowserInput

    data class Key(
        val event: KeyEvent,
        val key: String,
        val code: String,
        val modifiers: List<String>,
    ) : YoutubeRemoteBrowserInput

    data class Text(val value: String) : YoutubeRemoteBrowserInput
    data object Cancel : YoutubeRemoteBrowserInput
}

enum class PointerEvent { Down, Up, Move }

enum class KeyEvent { Down, Up }

interface YoutubeRemoteBrowserConnection {
    val state: StateFlow<YoutubeRemoteBrowserState>
    val frames: Flow<ByteArray>
    fun send(input: YoutubeRemoteBrowserInput): Boolean
    fun close()
}

interface YoutubeRemoteBrowserConnector {
    suspend fun connect(
        session: YoutubeRemoteBrowserSession,
    ): Result<YoutubeRemoteBrowserConnection>
}
