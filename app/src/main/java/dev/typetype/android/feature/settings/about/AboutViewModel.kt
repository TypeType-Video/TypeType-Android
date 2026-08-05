package dev.typetype.android.feature.settings.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.profile.ProfileRepository
import dev.typetype.android.domain.server.ServerRepository
import dev.typetype.android.domain.version.ComponentVersions
import dev.typetype.android.domain.version.ComponentVersionsRepository
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AboutState(
    val serverBaseUrl: String = "",
    val publicUsername: String = "",
    val role: String = "",
    val componentVersions: ComponentVersions? = null,
    val isLoadingVersions: Boolean = false,
)

private data class VersionLoadState(
    val versions: ComponentVersions? = null,
    val isLoading: Boolean = false,
)

@HiltViewModel
class AboutViewModel @Inject constructor(
    serverRepository: ServerRepository,
    profileRepository: ProfileRepository,
    private val componentVersionsRepository: ComponentVersionsRepository,
) : ViewModel() {
    private val versionLoadState = MutableStateFlow(VersionLoadState())
    private var versionRefreshJob: Job? = null

    val state = combine(
        serverRepository.observeCurrentServer(),
        profileRepository.observe(),
        versionLoadState,
    ) { server, profile, versions ->
        AboutState(
            serverBaseUrl = server?.baseUrl.orEmpty(),
            publicUsername = profile?.publicUsername.orEmpty(),
            role = profile?.role.orEmpty(),
            componentVersions = versions.versions,
            isLoadingVersions = versions.isLoading,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AboutState())

    init {
        viewModelScope.launch { profileRepository.refresh() }
        refreshVersions()
    }

    fun refreshVersions() {
        if (versionRefreshJob?.isActive == true) return
        versionRefreshJob = viewModelScope.launch {
            versionLoadState.value = versionLoadState.value.copy(isLoading = true)
            versionLoadState.value = componentVersionsRepository.fetch().fold(
                onSuccess = { VersionLoadState(versions = it) },
                onFailure = { versionLoadState.value.copy(isLoading = false) },
            )
        }
    }
}
