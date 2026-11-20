package dev.typetype.android.feature.player.livechat

import kotlinx.coroutines.flow.update
import dev.typetype.android.domain.twitch.TwitchChatAssets
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.livechat.LiveChatMessage
import dev.typetype.android.domain.twitch.TwitchRepository
import dev.typetype.android.domain.twitch.TwitchTarget
import dev.typetype.android.domain.twitch.TwitchUrls
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VodChatState(
    val messages: List<LiveChatMessage> = emptyList(),
    val ready: Boolean = false,
    val unavailable: Boolean = false,
    val assets: TwitchChatAssets = TwitchChatAssets(),
)

@HiltViewModel
class TwitchVodChatViewModel @Inject constructor(
    private val twitch: TwitchRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(VodChatState())
    val state: StateFlow<VodChatState> = _state.asStateFlow()
    private var videoId: String? = null
    private var window = VodChatWindow()
    private var loading = false
    private var retryAtMs = 0L

    fun start(videoUrl: String, channelUrl: String? = null) {
        val id = (TwitchUrls.parse(videoUrl) as? TwitchTarget.Vod)?.id
        if (id == videoId) return
        videoId = id
        window = VodChatWindow()
        _state.value = VodChatState()
        val login = channelUrl?.let { (TwitchUrls.parse(it) as? TwitchTarget.Live)?.login } ?: return
        viewModelScope.launch {
            twitch.chatAssets(login).onSuccess { assets -> _state.update { it.copy(assets = assets) } }
        }
    }

    fun onPosition(positionMs: Long) {
        val id = videoId ?: return
        val seconds = positionMs / 1_000
        publish(seconds)
        if (loading || System.currentTimeMillis() < retryAtMs) return
        val request = window.request(seconds) ?: return
        loading = true
        val target = window
        viewModelScope.launch {
            val cursor = (request as? VodChatRequest.Next)?.cursor
            twitch.videoComments(id, seconds.takeIf { cursor == null }, cursor).fold(
                onSuccess = { page ->
                    if (target === window) {
                        window.accept(request, page, seconds)
                        publish(seconds)
                    }
                },
                onFailure = {
                    retryAtMs = System.currentTimeMillis() + RETRY_DELAY_MS
                    if (target === window && !window.loaded) _state.update { it.copy(unavailable = true) }
                },
            )
            loading = false
        }
    }

    private fun publish(seconds: Long) {
        if (!window.loaded) return
        _state.update { state ->
            state.copy(
                messages = window.visible(seconds).map {
                    LiveChatMessage(
                        id = it.id, text = it.text, authorName = it.author, authorAvatarUrl = null, moderator = it.moderator,
                        login = it.login, color = it.color, emotes = it.emotes, badges = it.badges,
                    )
                },
                ready = true,
                unavailable = false,
            )
        }
    }
}

private const val RETRY_DELAY_MS = 15_000L
