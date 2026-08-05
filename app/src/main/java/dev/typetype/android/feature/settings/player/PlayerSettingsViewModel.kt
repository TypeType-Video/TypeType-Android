package dev.typetype.android.feature.settings.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.typetype.android.domain.preferences.PreferencesRepository
import dev.typetype.android.domain.usersettings.DEFAULT_SPONSOR_BLOCK_CATEGORY_ACTIONS
import dev.typetype.android.domain.usersettings.SponsorBlockMode
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
                    autoplayCountdownSeconds = prefs.playerAutoplayCountdownSeconds,
                    skipPlaylistAutoplayScreen = server.skipPlaylistAutoplayScreen,
                    pauseInBackgroundEnabled = prefs.playerPauseInBackground,
                    danmakuEnabled = prefs.danmakuEnabled,
                    danmakuSpeed = prefs.danmakuSpeed,
                    danmakuSize = prefs.danmakuSize,
                    defaultQuality = server.defaultQuality,
                    defaultPlaybackSpeed = server.defaultPlaybackSpeed,
                    defaultService = server.defaultService,
                    subtitlesEnabled = server.subtitlesEnabled,
                    defaultSubtitleLanguage = server.defaultSubtitleLanguage,
                    defaultAudioLanguage = server.defaultAudioLanguage,
                    preferOriginalLanguage = server.preferOriginalLanguage,
                    sponsorBlockMode = server.sponsorBlockMode,
                    sponsorBlockCategoryActions = server.sponsorBlockCategoryActions,
                    sponsorBlockMinimumDuration = server.sponsorBlockMinimumDuration,
                    sponsorBlockShowCurrentSegment = server.sponsorBlockShowCurrentSegment,
                    sponsorBlockShowChapters = server.sponsorBlockShowChapters,
                    sponsorBlockShowFullVideoLabels = server.sponsorBlockShowFullVideoLabels,
                    sponsorBlockManualSkipOnFullVideo = server.sponsorBlockManualSkipOnFullVideo,
                    sponsorBlockSkipNonMusicOnlyOnMusicVideos =
                        server.sponsorBlockSkipNonMusicOnlyOnMusicVideos,
                    sponsorBlockMuteInsteadOfSkip = server.sponsorBlockMuteInsteadOfSkip,
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
                is PlayerSettingsAction.SetDanmakuEnabled ->
                    preferencesRepository.setDanmakuEnabled(action.enabled)
                is PlayerSettingsAction.SetDanmakuSpeed ->
                    preferencesRepository.setDanmakuSpeed(action.speed)
                is PlayerSettingsAction.SetDanmakuSize ->
                    preferencesRepository.setDanmakuSize(action.size)
                is PlayerSettingsAction.SetAutoplay ->
                    updateServer { it.copy(autoplay = action.enabled) }
                is PlayerSettingsAction.SetAutoplayCountdown ->
                    preferencesRepository.setPlayerAutoplayCountdownSeconds(action.seconds)
                is PlayerSettingsAction.SetSkipPlaylistAutoplayScreen ->
                    updateServer { it.copy(skipPlaylistAutoplayScreen = action.enabled) }
                is PlayerSettingsAction.SetDefaultQuality ->
                    updateServer { it.copy(defaultQuality = action.quality) }
                is PlayerSettingsAction.SetDefaultPlaybackSpeed ->
                    updateServer { it.copy(defaultPlaybackSpeed = action.speed.coerceIn(0.25, 4.0)) }
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
                is PlayerSettingsAction.SetSponsorBlockMode -> updateServer {
                    it.withSponsorBlockMode(action.mode)
                }
                is PlayerSettingsAction.SetSponsorBlockCategory -> updateServer {
                    it.copy(
                        sponsorBlockCategoryActions = it.sponsorBlockCategoryActions +
                            (action.category to action.mode),
                    )
                }
                is PlayerSettingsAction.SetSponsorBlockMinimumDuration -> updateServer {
                    it.copy(sponsorBlockMinimumDuration = action.seconds.coerceAtLeast(0))
                }
                is PlayerSettingsAction.SetSponsorBlockOption -> updateServer {
                    it.withSponsorBlockOption(action.option, action.enabled)
                }
            }
        }
    }

    private suspend fun updateServer(transform: (UserSettings) -> UserSettings) {
        userSettingsRepository.update(transform)
    }
}

internal fun UserSettings.withSponsorBlockMode(mode: SponsorBlockMode): UserSettings = copy(
    sponsorBlockMode = mode,
    sponsorBlockCategoryActions = DEFAULT_SPONSOR_BLOCK_CATEGORY_ACTIONS.mapValues { mode },
)

internal fun UserSettings.withSponsorBlockOption(
    option: SponsorBlockOption,
    enabled: Boolean,
): UserSettings = when (option) {
    SponsorBlockOption.ShowCurrentSegment -> copy(sponsorBlockShowCurrentSegment = enabled)
    SponsorBlockOption.ShowChapters -> copy(sponsorBlockShowChapters = enabled)
    SponsorBlockOption.ShowFullVideoLabels -> copy(sponsorBlockShowFullVideoLabels = enabled)
    SponsorBlockOption.ManualSkipOnFullVideo -> copy(sponsorBlockManualSkipOnFullVideo = enabled)
    SponsorBlockOption.SkipNonMusicOnlyOnMusicVideos -> copy(
        sponsorBlockSkipNonMusicOnlyOnMusicVideos = enabled,
    )
    SponsorBlockOption.MuteInsteadOfSkip -> copy(sponsorBlockMuteInsteadOfSkip = enabled)
}
