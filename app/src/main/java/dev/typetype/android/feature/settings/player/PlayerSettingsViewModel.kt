package dev.typetype.android.feature.settings.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.preferences.PreferencesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerSettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerSettingsState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.observe().collect { prefs ->
                _state.update {
                    PlayerSettingsState(
                        doubleTapSeekEnabled = prefs.playerDoubleTapSeekEnabled,
                        swipeSeekEnabled = prefs.playerSwipeSeekEnabled,
                        swipeBrightnessVolumeEnabled = prefs.playerSwipeBrightnessVolumeEnabled,
                        longPressSpeedEnabled = prefs.playerLongPressSpeedEnabled,
                        autoplayEnabled = prefs.playerAutoplayEnabled,
                        pauseInBackgroundEnabled = prefs.playerPauseInBackground,
                    )
                }
            }
        }
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
                is PlayerSettingsAction.SetAutoplay ->
                    preferencesRepository.setPlayerAutoplayEnabled(action.enabled)
                is PlayerSettingsAction.SetPauseInBackground ->
                    preferencesRepository.setPlayerPauseInBackground(action.enabled)
            }
        }
    }
}
