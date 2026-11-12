package dev.typetype.android.feature.settings.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.domain.profiles.AccountProfile
import dev.typetype.android.domain.profiles.AccountProfilesRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountProfilesState(
    val profiles: List<AccountProfile> = emptyList(),
    val available: Boolean = false,
    val busy: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class AccountProfilesViewModel @Inject constructor(
    private val repository: AccountProfilesRepository,
    private val errorMapper: UserErrorMapper,
) : ViewModel() {
    private val _state = MutableStateFlow(AccountProfilesState())
    val state: StateFlow<AccountProfilesState> = _state.asStateFlow()
    private val switched = Channel<Unit>(Channel.CONFLATED)
    val profileSwitched: Flow<Unit> = switched.receiveAsFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            repository.profiles().onSuccess { result ->
                _state.update { it.copy(profiles = result.profiles, available = true) }
            }
        }
    }

    fun create(name: String) = write { repository.create(name).getOrThrow() }

    fun rename(profileId: String, name: String) = write { repository.rename(profileId, name).getOrThrow() }

    fun setDefault(profileId: String) = write { repository.setDefault(profileId).getOrThrow() }

    fun delete(profileId: String) = write { repository.delete(profileId).getOrThrow() }

    fun switchTo(profileId: String) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, errorMessage = null) }
        viewModelScope.launch {
            repository.switchTo(profileId).fold(
                onSuccess = { switched.trySend(Unit) },
                onFailure = ::showFailure,
            )
            _state.update { it.copy(busy = false) }
        }
    }

    private fun write(block: suspend () -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                showFailure(failure)
            }
            repository.profiles().onSuccess { result -> _state.update { it.copy(profiles = result.profiles) } }
            _state.update { it.copy(busy = false) }
        }
    }

    private fun showFailure(failure: Throwable) {
        val message = errorMapper.details(failure, R.string.account_profiles_failed).message
        _state.update { it.copy(errorMessage = message) }
    }
}
