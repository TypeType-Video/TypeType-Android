package dev.typetype.android.feature.player.twitch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.twitch.TwitchAccountRepository
import dev.typetype.android.domain.twitch.TwitchChannel
import dev.typetype.android.domain.twitch.TwitchRepository
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TwitchLiveDetailsState(
    val channel: TwitchChannel? = null,
    val followable: Boolean = false,
    val following: Boolean = false,
)

// The channel behind a live, read again every minute like the web watch page so the viewer count and the category
// follow the stream; following goes through the linked Twitch account when there is one.
@HiltViewModel
class TwitchLiveDetailsViewModel @Inject constructor(
    private val twitch: TwitchRepository,
    private val accounts: TwitchAccountRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(TwitchLiveDetailsState())
    val state: StateFlow<TwitchLiveDetailsState> = _state.asStateFlow()
    private var job: Job? = null
    private var login: String? = null

    fun start(login: String) {
        if (this.login == login && job?.isActive == true) return
        this.login = login
        job?.cancel()
        _state.value = TwitchLiveDetailsState()
        job = viewModelScope.launch {
            launch {
                if (accounts.status().getOrNull()?.linked != true) return@launch
                accounts.isFollowing(login).onSuccess { following ->
                    _state.update { it.copy(followable = true, following = following) }
                }
            }
            while (isActive) {
                twitch.channel(login).onSuccess { channel -> _state.update { it.copy(channel = channel) } }
                delay(REFRESH_MS)
            }
        }
    }

    fun toggleFollow() {
        val target = login ?: return
        val current = _state.value
        if (!current.followable) return
        _state.update { it.copy(following = !current.following) }
        viewModelScope.launch {
            accounts.setFollowing(target, !current.following).onFailure {
                _state.update { it.copy(following = current.following) }
            }
        }
    }

    override fun onCleared() {
        job?.cancel()
    }

    private companion object {
        const val REFRESH_MS = 60_000L
    }
}
