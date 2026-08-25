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

    fun onAction(action: AppearanceAction) {
        viewModelScope.launch {
            when (action) {
                is AppearanceAction.SelectAccent -> preferencesRepository.setAccentColor(action.accent)
                is AppearanceAction.SelectPersonality ->
                    preferencesRepository.setAppearancePersonality(action.personality)
                is AppearanceAction.SelectMode -> preferencesRepository.setAppearanceMode(action.mode)
                is AppearanceAction.SetAmoled -> preferencesRepository.setAppearanceAmoled(action.enabled)
                is AppearanceAction.SelectFont -> preferencesRepository.setAppearanceFont(action.font)
                is AppearanceAction.SelectMotion -> preferencesRepository.setAppearanceMotion(action.motion)
                is AppearanceAction.SelectMangaPaper -> preferencesRepository.setMangaPaper(action.paper)
                is AppearanceAction.SelectHeadlineMarker ->
                    preferencesRepository.setMangaHeadlineMarker(action.marker)
                is AppearanceAction.SetMangaDecoration -> setMangaDecoration(action)
            }
        }
    }

    fun selectAccent(accent: AccentColor) {
        onAction(AppearanceAction.SelectAccent(accent))
    }

    private suspend fun setMangaDecoration(action: AppearanceAction.SetMangaDecoration) {
        when (action.decoration) {
            MangaDecoration.Screentone -> preferencesRepository.setMangaScreentone(action.enabled)
            MangaDecoration.SpeedLines -> preferencesRepository.setMangaSpeedLines(action.enabled)
            MangaDecoration.Starburst -> preferencesRepository.setMangaStarburst(action.enabled)
            MangaDecoration.InkedIcons -> preferencesRepository.setMangaInkedIcons(action.enabled)
            MangaDecoration.PanelTilt -> preferencesRepository.setMangaPanelTilt(action.enabled)
        }
    }
}
