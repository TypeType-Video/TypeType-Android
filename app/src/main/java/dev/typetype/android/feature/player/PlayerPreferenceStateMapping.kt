package dev.typetype.android.feature.player

internal fun PlayerState.applyPreferences(prefs: PlayerPreferenceState): PlayerState = copy(
    gestureConfig = prefs.gestureConfig,
    playbackBrightnessPercent = prefs.brightnessPercent,
    autoplayCountdownSeconds = prefs.autoplayCountdownSeconds,
    audioOnlyPlaybackDefault = prefs.audioOnlyPlaybackDefault,
    preferredCodec = prefs.preferredCodec,
    userSettings = prefs.userSettings,
)
