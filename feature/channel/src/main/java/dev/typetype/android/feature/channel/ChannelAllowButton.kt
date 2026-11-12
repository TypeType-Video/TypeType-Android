package dev.typetype.android.feature.channel

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.R
import dev.typetype.android.core.ui.navigation.ChannelRoute
import dev.typetype.android.data.account.ActiveAccountScope
import dev.typetype.android.domain.account.AccountRepository
import dev.typetype.android.domain.allowlist.AllowedChannelsRepository
import dev.typetype.android.domain.channel.Channel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChannelAllowState(val canAllow: Boolean = false, val allowed: Boolean = false, val busy: Boolean = false)

@HiltViewModel
class ChannelAllowViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    accountRepository: AccountRepository,
    activeAccountScope: ActiveAccountScope,
    private val repository: AllowedChannelsRepository,
) : ViewModel() {
    private val channelUrl = savedStateHandle.toRoute<ChannelRoute>().channelUrl
    private val progress = MutableStateFlow(ChannelAllowState())

    val state: StateFlow<ChannelAllowState> = combine(
        accountRepository.observeAccounts(),
        activeAccountScope.observe(),
        progress,
    ) { accounts, scope, current ->
        val role = accounts.firstOrNull { it.serverId == scope?.serverId && it.id == scope.accountId }?.role
        current.copy(canAllow = role == "admin" || role == "moderator")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChannelAllowState())

    init {
        viewModelScope.launch {
            repository.allowedChannelUrls().onSuccess { urls ->
                progress.value = progress.value.copy(allowed = channelUrl in urls)
            }
        }
    }

    fun allow(channel: Channel) {
        if (progress.value.busy || progress.value.allowed) return
        progress.value = progress.value.copy(busy = true)
        viewModelScope.launch {
            val done = repository.allowForEveryone(channelUrl, channel.name, channel.avatarUrl.ifBlank { null }).isSuccess
            progress.value = progress.value.copy(busy = false, allowed = done)
        }
    }
}

@Composable
internal fun ChannelAllowButton(channel: Channel, viewModel: ChannelAllowViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (!state.canAllow) return
    FilledTonalButton(
        onClick = { viewModel.allow(channel) },
        enabled = !state.allowed && !state.busy,
        modifier = Modifier.padding(horizontal = 8.dp),
    ) {
        Icon(if (state.allowed) Icons.Filled.Check else Icons.Outlined.FamilyRestroom, contentDescription = null)
        Text(
            text = stringResource(if (state.allowed) R.string.channel_allowed else R.string.channel_allow),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
