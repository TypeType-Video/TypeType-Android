package dev.typetype.android.feature.settings.linkedaccounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.domain.server.ServerRepository
import dev.typetype.android.domain.twitch.TwitchAccountRepository
import dev.typetype.android.domain.twitch.TwitchLinkCode
import dev.typetype.android.domain.twitch.TwitchLinkPoll
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TwitchLinkState(
    val available: Boolean = false,
    val linkedName: String? = null,
    val linked: Boolean = false,
    val code: TwitchLinkCode? = null,
    val busy: Boolean = false,
    val expired: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class TwitchLinkViewModel @Inject constructor(
    private val servers: ServerRepository,
    private val accounts: TwitchAccountRepository,
    private val errorMapper: UserErrorMapper,
) : ViewModel() {
    private val _state = MutableStateFlow(TwitchLinkState())
    val state = _state.asStateFlow()
    private var linkJob: Job? = null

    init {
        viewModelScope.launch {
            if (servers.observeCurrentServer().first()?.twitchEnabled != true) return@launch
            accounts.status().onSuccess { status ->
                _state.update { it.copy(available = true, linked = status.linked, linkedName = status.name) }
            }
        }
    }

    // Polls at the pace Twitch asks for, slower on slow_down, until the code is confirmed or expires, like the web.
    fun start() {
        linkJob?.cancel()
        _state.update { it.copy(error = null, expired = false, code = null) }
        linkJob = viewModelScope.launch {
            val code = accounts.start().getOrElse { return@launch fail(it) }
            _state.update { it.copy(code = code) }
            val deadline = System.currentTimeMillis() + code.expiresInSeconds * 1_000L
            var wait = code.intervalSeconds * 1_000L
            while (System.currentTimeMillis() < deadline) {
                delay(wait)
                when (val poll = accounts.poll(code.deviceCode).getOrElse { return@launch fail(it) }) {
                    is TwitchLinkPoll.Linked -> {
                        _state.update { it.copy(code = null, linked = true, linkedName = poll.name) }
                        return@launch
                    }
                    TwitchLinkPoll.SlowDown -> wait += SLOW_DOWN_MS
                    TwitchLinkPoll.Pending -> Unit
                }
            }
            _state.update { it.copy(code = null, expired = true) }
        }
    }

    fun cancel() {
        linkJob?.cancel()
        _state.update { it.copy(code = null) }
    }

    fun disconnect() {
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            accounts.unlink().fold(
                onSuccess = { _state.update { it.copy(busy = false, linked = false, linkedName = null) } },
                onFailure = { failure ->
                    _state.update { it.copy(busy = false) }
                    fail(failure)
                },
            )
        }
    }

    private fun fail(failure: Throwable) = _state.update {
        it.copy(code = null, error = errorMapper.message(failure, R.string.linked_accounts_twitch_failed))
    }

    private companion object {
        const val SLOW_DOWN_MS = 5_000L
    }
}
