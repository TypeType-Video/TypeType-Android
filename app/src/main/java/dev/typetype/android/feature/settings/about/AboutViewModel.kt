package dev.typetype.android.feature.settings.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.profile.ProfileRepository
import dev.typetype.android.domain.server.ServerRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AboutState(
    val serverBaseUrl: String = "",
    val publicUsername: String = "",
    val role: String = "",
)

@HiltViewModel
class AboutViewModel @Inject constructor(
    serverRepository: ServerRepository,
    profileRepository: ProfileRepository,
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(Unit)

    val state = combine(
        serverRepository.observeCurrentServer(),
        profileRepository.observe(),
    ) { server, profile ->
        AboutState(
            serverBaseUrl = server?.baseUrl.orEmpty(),
            publicUsername = profile?.publicUsername.orEmpty(),
            role = profile?.role.orEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AboutState())

    init {
        viewModelScope.launch { profileRepository.refresh() }
    }
}
