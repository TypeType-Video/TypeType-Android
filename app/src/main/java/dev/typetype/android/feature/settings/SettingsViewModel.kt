package dev.typetype.android.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.server.ServerCapabilitiesRepository
import dev.typetype.android.domain.server.ServerRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsState(
    val rssAvailable: Boolean = false,
    val youtubeSessionAvailable: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val capabilitiesRepository: ServerCapabilitiesRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            serverRepository.observeCurrentServer()
                .distinctUntilChangedBy { it?.id }
                .collect { cached ->
                    val server = cached?.let {
                        capabilitiesRepository.refresh(it.id).getOrDefault(it)
                    }
                    mutableState.update {
                        SettingsState(
                            rssAvailable = server?.rss?.enabled == true,
                            youtubeSessionAvailable =
                                server?.youtubeRemoteLoginSupported == true,
                        )
                    }
                }
        }
    }
}
