package dev.typetype.android.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.data.network.AccessTokenStore
import dev.typetype.android.domain.server.ServerRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val tokenStore: AccessTokenStore,
) : ViewModel() {

    val state: StateFlow<HomeState> = serverRepository.observeCurrentServer()
        .map { HomeState(currentServer = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeState(),
        )

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.OnSignOutClick -> signOut()
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            tokenStore.setAccessToken(null)
            serverRepository.clearCurrentServer()
        }
    }
}
