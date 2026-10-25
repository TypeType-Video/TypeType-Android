package dev.typetype.android.feature.player

import dev.typetype.android.data.network.PlaybackNetworkState
import dev.typetype.android.domain.preferences.PreferencesRepository
import dev.typetype.android.domain.preferences.effectivePlaybackQualityCap
import dev.typetype.android.domain.usersettings.UserSettings
import dev.typetype.android.domain.usersettings.UserSettingsRepository
import dev.typetype.android.feature.player.components.PlayerGestureConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal class PlayerPreferenceCoordinator(
    private val local: PreferencesRepository,
    private val server: UserSettingsRepository,
    private val networkStates: StateFlow<PlaybackNetworkState>,
    private val scope: CoroutineScope,
) {
    val states: Flow<PlayerPreferenceState> = combine(
        local.observe(),
        server.observe(),
        networkStates,
    ) { preferences, userSettings, network ->
        val automaticQualityCap = effectivePlaybackQualityCap(
            defaultQuality = preferences.playerDefaultQuality,
            mobileDataQuality = preferences.playerMobileDataQuality,
            isMetered = network.isMetered,
        )
        PlayerPreferenceState(
            gestureConfig = PlayerGestureConfig(
                doubleTapSeekEnabled = preferences.playerDoubleTapSeekEnabled,
                doubleTapSeekSeconds = preferences.playerDoubleTapSeekSeconds,
                swipeSeekEnabled = preferences.playerSwipeSeekEnabled,
                swipeBrightnessVolumeEnabled = preferences.playerSwipeBrightnessVolumeEnabled,
                longPressSpeedEnabled = preferences.playerLongPressSpeedEnabled,
                accessibleControlsEnabled = preferences.playerAccessibleControlsEnabled,
            ),
            brightnessPercent = preferences.playerPlaybackBrightnessPercent,
            autoplayCountdownSeconds = preferences.playerAutoplayCountdownSeconds,
            audioOnlyPlaybackDefault = preferences.playerAudioOnlyPlayback,
            preferredCodec = preferences.playerPreferredCodec,
            userSettings = userSettings.copy(defaultQuality = automaticQualityCap),
        )
    }

    private var brightnessJob: Job? = null
    private var autoplayJob: Job? = null

    suspend fun refresh() = server.refresh()

    fun updateBrightness(percent: Int, onChanged: (Int) -> Unit) {
        val value = percent.coerceIn(0, 100)
        onChanged(value)
        brightnessJob?.cancel()
        brightnessJob = scope.launch {
            delay(BRIGHTNESS_SAVE_DEBOUNCE_MS)
            local.setPlayerPlaybackBrightnessPercent(value)
        }
    }

    fun updateAutoplay(
        enabled: Boolean,
        onChanged: (Boolean) -> Unit,
        onFailure: () -> Unit,
    ) {
        autoplayJob?.cancel()
        onChanged(enabled)
        autoplayJob = scope.launch {
            server.update { it.copy(autoplay = enabled) }.onFailure { onFailure() }
        }
    }

    fun updatePreferredCodec(codec: String) {
        scope.launch { local.setPlayerPreferredCodec(codec) }
    }
}

internal data class PlayerPreferenceState(
    val gestureConfig: PlayerGestureConfig,
    val brightnessPercent: Int?,
    val autoplayCountdownSeconds: Int,
    val audioOnlyPlaybackDefault: Boolean,
    val preferredCodec: String,
    val userSettings: UserSettings,
)

private const val BRIGHTNESS_SAVE_DEBOUNCE_MS = 180L
