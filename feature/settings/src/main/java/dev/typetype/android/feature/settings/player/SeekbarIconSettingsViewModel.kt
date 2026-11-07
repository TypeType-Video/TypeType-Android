package dev.typetype.android.feature.settings.player

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.usersettings.SEEKBAR_ICON_TYPETYPE
import dev.typetype.android.domain.usersettings.SeekbarIconRepository
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SeekbarIconError { Unsupported, TooLarge, Failed }

data class SeekbarIconSettingsState(
    val icon: String = SEEKBAR_ICON_TYPETYPE,
    val customUrl: String? = null,
    val busy: Boolean = false,
    val error: SeekbarIconError? = null,
)

@HiltViewModel
class SeekbarIconSettingsViewModel @Inject constructor(
    private val userSettings: UserSettingsRepository,
    private val icons: SeekbarIconRepository,
    private val reader: SeekbarIconSelectionReader,
) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val error = MutableStateFlow<SeekbarIconError?>(null)

    val state: StateFlow<SeekbarIconSettingsState> =
        combine(userSettings.observe(), busy, error) { settings, isBusy, lastError ->
            SeekbarIconSettingsState(settings.seekbarIcon, settings.seekbarIconUrl, isBusy, lastError)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SeekbarIconSettingsState())

    fun select(icon: String) = run {
        userSettings.update { it.copy(seekbarIcon = icon) }
    }

    fun upload(uri: Uri) = run {
        when (val selection = withContext(Dispatchers.IO) { reader.read(uri) }) {
            is SeekbarIconSelection.Ready -> icons.upload(selection.upload)
            SeekbarIconSelection.TooLarge -> Result.failure(SeekbarIconException(SeekbarIconError.TooLarge))
            SeekbarIconSelection.Unsupported -> Result.failure(SeekbarIconException(SeekbarIconError.Unsupported))
        }
    }

    fun remove() = run { icons.delete() }

    private fun run(block: suspend () -> Result<Unit>) {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            error.value = null
            block().onFailure { failure ->
                error.value = (failure as? SeekbarIconException)?.error ?: SeekbarIconError.Failed
            }
            busy.value = false
        }
    }
}

private class SeekbarIconException(val error: SeekbarIconError) : Exception(error.name)
