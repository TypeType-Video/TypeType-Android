package dev.typetype.android.feature.player.livechat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.twitch.TwitchAccountRepository
import dev.typetype.android.domain.twitch.TwitchChatHighlights
import dev.typetype.android.domain.twitch.TwitchRepository
import dev.typetype.android.domain.twitch.TwitchTarget
import dev.typetype.android.domain.twitch.TwitchUrls
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class TwitchChatHighlightsViewModel @Inject constructor(
    private val twitch: TwitchRepository,
    private val accounts: TwitchAccountRepository,
) : ViewModel() {
    // A vote needs the linked Twitch account; a poll is voted once, further votes would cost channel points.
    private val _canVote = MutableStateFlow(false)
    val canVote: StateFlow<Boolean> = _canVote.asStateFlow()
    private val _voted = MutableStateFlow<Set<String>>(emptySet())
    val voted: StateFlow<Set<String>> = _voted.asStateFlow()
    private val highlights = MutableStateFlow(TwitchChatHighlights(null, null))
    private val hidden = MutableStateFlow<Set<String>>(emptySet())
    private val _state = MutableStateFlow(TwitchChatHighlights(null, null))
    val state: StateFlow<TwitchChatHighlights> = _state.asStateFlow()
    private var job: Job? = null

    // Twitch only pushes polls and pins to its own PubSub, so the web reads them every ten seconds; so does this.
    fun start(videoUrl: String) {
        job?.cancel()
        val login = (TwitchUrls.parse(videoUrl) as? TwitchTarget.Live)?.login ?: return
        viewModelScope.launch { _canVote.value = accounts.status().getOrNull()?.linked == true }
        job = viewModelScope.launch {
            while (isActive) {
                twitch.chatHighlights(login).onSuccess { found ->
                    highlights.value = found
                    publish()
                }
                delay(REFRESH_MS)
            }
        }
    }

    fun vote(pollId: String, choiceId: String) {
        if (pollId in _voted.value) return
        _voted.update { it + pollId }
        viewModelScope.launch { accounts.votePoll(pollId, choiceId).onFailure { _voted.update { it - pollId } } }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    // A hidden poll or pin stays hidden only until the channel starts another one, as on Twitch.
    fun hide(id: String) {
        hidden.update { it + id }
        publish()
    }

    private fun publish() {
        val current = highlights.value
        val gone = hidden.value
        _state.value = TwitchChatHighlights(
            poll = current.poll?.takeUnless { it.id in gone },
            pinned = current.pinned?.takeUnless { it.id in gone },
        )
    }

    override fun onCleared() {
        stop()
    }

    private companion object {
        const val REFRESH_MS = 10_000L
    }
}
