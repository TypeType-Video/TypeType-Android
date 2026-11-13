package dev.typetype.android.feature.subscriptions.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.error.UserErrorMapper
import dev.typetype.android.domain.subscriptions.SubscriptionGroup
import dev.typetype.android.domain.subscriptions.SubscriptionGroupsRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SubscriptionGroupsState(
    val groups: List<SubscriptionGroup> = emptyList(),
    val loaded: Boolean = false,
    val busy: Boolean = false,
    val errorMessage: String? = null,
    val editedChannelUrl: String? = null,
    val editedChannelGroupIds: Set<String>? = null,
)

@HiltViewModel
class SubscriptionGroupsViewModel @Inject constructor(
    private val repository: SubscriptionGroupsRepository,
    private val errorMapper: UserErrorMapper,
) : ViewModel() {
    private val _state = MutableStateFlow(SubscriptionGroupsState())
    val state: StateFlow<SubscriptionGroupsState> = _state.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            repository.groups().fold(
                onSuccess = { groups -> _state.update { it.copy(groups = groups, loaded = true) } },
                onFailure = ::showFailure,
            )
        }
    }

    fun create(name: String, thenAddChannel: String? = null) = write {
        val group = repository.create(name).getOrThrow()
        if (thenAddChannel != null) {
            repository.setMember(group.id, thenAddChannel, member = true).getOrThrow()
            _state.update { state ->
                if (state.editedChannelUrl != thenAddChannel) return@update state
                state.copy(editedChannelGroupIds = state.editedChannelGroupIds.orEmpty() + group.id)
            }
        }
    }

    fun rename(groupId: String, name: String) = write { repository.rename(groupId, name).getOrThrow() }

    fun delete(groupId: String) = write { repository.delete(groupId).getOrThrow() }

    fun editChannel(channelUrl: String?) {
        _state.update { it.copy(editedChannelUrl = channelUrl, editedChannelGroupIds = null) }
        channelUrl ?: return
        viewModelScope.launch {
            repository.groupIdsOf(channelUrl).fold(
                onSuccess = { ids ->
                    _state.update { if (it.editedChannelUrl == channelUrl) it.copy(editedChannelGroupIds = ids) else it }
                },
                onFailure = ::showFailure,
            )
        }
    }

    fun setMember(groupId: String, member: Boolean) {
        val channelUrl = _state.value.editedChannelUrl ?: return
        write {
            repository.setMember(groupId, channelUrl, member).getOrThrow()
            _state.update { state ->
                val ids = state.editedChannelGroupIds.orEmpty()
                state.copy(editedChannelGroupIds = if (member) ids + groupId else ids - groupId)
            }
        }
    }

    fun dismissError() = _state.update { it.copy(errorMessage = null) }

    private fun write(block: suspend () -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { block() }.onFailure(::showFailure)
            repository.groups().onSuccess { groups -> _state.update { it.copy(groups = groups, loaded = true) } }
            _state.update { it.copy(busy = false) }
        }
    }

    private fun showFailure(failure: Throwable) {
        if (failure is CancellationException) throw failure
        val message = errorMapper.details(
            failure,
            R.string.subscription_groups_failed,
            missingRes = R.string.error_subscription_group_not_found,
        ).message
        _state.update { it.copy(errorMessage = message) }
    }
}
