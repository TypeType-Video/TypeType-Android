package dev.typetype.android.feature.settings.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.data.account.AccountScope
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.domain.account.Account
import dev.typetype.android.domain.account.AccountRepository
import dev.typetype.android.domain.server.Server
import dev.typetype.android.domain.server.ServerRepository
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountSettingsState(
    val servers: List<Server> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val activeScope: AccountScope? = null,
    val busyAccountId: String? = null,
    val errorMessage: String? = null,
    val errorRequestId: String? = null,
)

sealed interface AccountSettingsEvent {
    data object AccountActivated : AccountSettingsEvent
    data class SignIn(val serverId: String) : AccountSettingsEvent
    data object AddInstance : AccountSettingsEvent
}

@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    private val errorMapper: UserErrorMapper,
    serverRepository: ServerRepository,
    private val accountRepository: AccountRepository,
    activeAccountScope: ActiveAccountScope,
) : ViewModel() {
    private val _state = MutableStateFlow(AccountSettingsState())
    val state = _state.asStateFlow()

    private val eventChannel = Channel<AccountSettingsEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(
                serverRepository.observeServers(),
                accountRepository.observeAccounts(),
                activeAccountScope.observe(),
            ) { servers, accounts, active -> Triple(servers, accounts, active) }
                .collect { (servers, accounts, active) ->
                    _state.update { it.copy(servers = servers, accounts = accounts, activeScope = active) }
                }
        }
    }

    fun select(serverId: String, accountId: String) {
        if (_state.value.busyAccountId != null) return
        viewModelScope.launch {
            _state.update {
                it.copy(busyAccountId = accountId, errorMessage = null, errorRequestId = null)
            }
            accountRepository.select(serverId, accountId)
                .onSuccess { eventChannel.send(AccountSettingsEvent.AccountActivated) }
                .onFailure { error ->
                    val details = errorMapper.details(error, R.string.accounts_switch_failed)
                    _state.update {
                        it.copy(
                            errorMessage = details.message,
                            errorRequestId = details.requestId,
                        )
                    }
                }
            _state.update { it.copy(busyAccountId = null) }
        }
    }

    fun forget(serverId: String, accountId: String) {
        if (_state.value.busyAccountId != null) return
        viewModelScope.launch {
            _state.update {
                it.copy(busyAccountId = accountId, errorMessage = null, errorRequestId = null)
            }
            accountRepository.forget(serverId, accountId)
                .onFailure { error ->
                    val details = errorMapper.details(error, R.string.accounts_forget_failed)
                    _state.update {
                        it.copy(
                            errorMessage = details.message,
                            errorRequestId = details.requestId,
                        )
                    }
                }
            _state.update { it.copy(busyAccountId = null) }
        }
    }

    fun signIn(serverId: String) {
        viewModelScope.launch { eventChannel.send(AccountSettingsEvent.SignIn(serverId)) }
    }

    fun addInstance() {
        viewModelScope.launch { eventChannel.send(AccountSettingsEvent.AddInstance) }
    }
}
