package dev.typetype.android.feature.settings.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.preferences.PreferencesRepository
import dev.typetype.android.domain.usersettings.UserSettings
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerSettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val userSettingsRepository: UserSettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerSettingsState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferencesRepository.observe(),
                userSettingsRepository.observe(),
            ) { prefs, server ->
                PlayerSettingsState(
                    doubleTapSeekEnabled = prefs.playerDoubleTapSeekEnabled,
                    swipeSeekEnabled = prefs.playerSwipeSeekEnabled,
                    swipeBrightnessVolumeEnabled = prefs.playerSwipeBrightnessVolumeEnabled,
                    longPressSpeedEnabled = prefs.playerLongPressSpeedEnabled,
                    autoplayEnabled = server.autoplay,
                    pauseInBackgroundEnabled = prefs.playerPauseInBackground,
                    defaultQuality = server.defaultQuality,
                    defaultService = server.defaultService,
                    subtitlesEnabled = server.subtitlesEnabled,
                    defaultSubtitleLanguage = server.defaultSubtitleLanguage,
                    defaultAudioLanguage = server.defaultAudioLanguage,
                    preferOriginalLanguage = server.preferOriginalLanguage,
                )
            }.collect { merged -> _state.update { merged } }
        }
        viewModelScope.launch { userSettingsRepository.refresh() }
    }

    fun onAction(action: PlayerSettingsAction) {
        viewModelScope.launch {
            when (action) {
                is PlayerSettingsAction.SetDoubleTapSeek ->
                    preferencesRepository.setPlayerDoubleTapSeekEnabled(action.enabled)
                is PlayerSettingsAction.SetSwipeSeek ->
                    preferencesRepository.setPlayerSwipeSeekEnabled(action.enabled)
                is PlayerSettingsAction.SetSwipeBrightnessVolume ->
                    preferencesRepository.setPlayerSwipeBrightnessVolumeEnabled(action.enabled)
                is PlayerSettingsAction.SetLongPressSpeed ->
                    preferencesRepository.setPlayerLongPressSpeedEnabled(action.enabled)
                is PlayerSettingsAction.SetPauseInBackground ->
                    preferencesRepository.setPlayerPauseInBackground(action.enabled)
                is PlayerSettingsAction.SetAutoplay ->
                    updateServer { it.copy(autoplay = action.enabled) }
                is PlayerSettingsAction.SetDefaultQuality ->
                    updateServer { it.copy(defaultQuality = action.quality) }
                is PlayerSettingsAction.SetDefaultService ->
                    updateServer { it.copy(defaultService = action.service) }
                is PlayerSettingsAction.SetSubtitlesEnabled ->
                    updateServer { it.copy(subtitlesEnabled = action.enabled) }
                is PlayerSettingsAction.SetSubtitleLanguage ->
                    updateServer { it.copy(defaultSubtitleLanguage = action.language) }
                is PlayerSettingsAction.SetAudioLanguage ->
                    updateServer { it.copy(defaultAudioLanguage = action.language) }
                is PlayerSettingsAction.SetPreferOriginalLanguage ->
                    updateServer { it.copy(preferOriginalLanguage = action.enabled) }
            }
        }
    }

    private suspend fun updateServer(transform: (UserSettings) -> UserSettings) {
        userSettingsRepository.update(transform)
    }
}
