package dev.typetype.android.feature.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.preferences.AccentColor
import dev.typetype.android.domain.preferences.AppPreferences
import dev.typetype.android.domain.preferences.PreferencesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val state: StateFlow<AppPreferences> = preferencesRepository.observe()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppPreferences(),
        )

    fun selectAccent(accent: AccentColor) {
        viewModelScope.launch {
            preferencesRepository.setAccentColor(accent)
        }
    }
}
