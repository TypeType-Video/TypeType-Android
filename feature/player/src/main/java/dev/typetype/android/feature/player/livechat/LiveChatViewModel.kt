package dev.typetype.android.feature.player.livechat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.livechat.LiveChatEvent
import dev.typetype.android.domain.livechat.LiveChatMessage
import dev.typetype.android.domain.livechat.LiveChatRepository
import dev.typetype.android.domain.livechat.LiveChatRequestException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LiveChatStatus { Connecting, Live, Reconnecting, Ended, Failed }

data class LiveChatState(
    val messages: List<LiveChatMessage> = emptyList(),
    val status: LiveChatStatus = LiveChatStatus.Connecting,
)

@HiltViewModel
class LiveChatViewModel @Inject constructor(
    private val repository: LiveChatRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LiveChatState())
    val state: StateFlow<LiveChatState> = _state.asStateFlow()
    private var job: Job? = null

    fun start(videoUrl: String) {
        job?.cancel()
        _state.value = LiveChatState()
        job = viewModelScope.launch { follow(videoUrl) }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun retry(videoUrl: String) {
        if (_state.value.status == LiveChatStatus.Failed) start(videoUrl)
    }

    private suspend fun follow(videoUrl: String) {
        var attempt = 0
        while (true) {
            val failure = try {
                repository.connect(videoUrl).collect { event ->
                    attempt = 0
                    when (event) {
                        LiveChatEvent.Connected -> _state.update { it.copy(status = LiveChatStatus.Live) }
                        is LiveChatEvent.Message -> _state.update { it.withMessage(event.message) }
                        LiveChatEvent.Ended -> _state.update { it.copy(status = LiveChatStatus.Ended) }
                    }
                }
                null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                error
            }
            if (_state.value.status == LiveChatStatus.Ended) return
            val wait = liveChatRetryDelayMillis(failure, attempt)
            if (wait == null) {
                _state.update { it.copy(status = LiveChatStatus.Failed) }
                return
            }
            _state.update { it.copy(status = LiveChatStatus.Reconnecting) }
            delay(wait)
            attempt += 1
        }
    }

    override fun onCleared() {
        stop()
    }
}

private fun LiveChatState.withMessage(message: LiveChatMessage): LiveChatState {
    if (messages.any { it.id == message.id }) return this
    return copy(messages = (messages + message).takeLast(MAX_LIVE_CHAT_MESSAGES))
}

// Same policy as the web client: give up after five attempts or on a client error the server will repeat.
internal fun liveChatRetryDelayMillis(failure: Exception?, attempt: Int): Long? {
    if (attempt >= MAX_RETRIES) return null
    val status = (failure as? LiveChatRequestException)?.status
    if (status != null && status != 408 && status != 429 && status < 500) return null
    return minOf(1_000L shl attempt, MAX_RETRY_DELAY_MS)
}

internal const val MAX_LIVE_CHAT_MESSAGES = 200
private const val MAX_RETRIES = 5
private const val MAX_RETRY_DELAY_MS = 15_000L
