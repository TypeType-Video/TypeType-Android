package dev.typetype.android.domain.preferences

import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    fun observe(): Flow<AppPreferences>
    suspend fun setAccentColor(accentColor: AccentColor)
    suspend fun setPlayerDoubleTapSeekEnabled(enabled: Boolean)
    suspend fun setPlayerSwipeSeekEnabled(enabled: Boolean)
    suspend fun setPlayerSwipeBrightnessVolumeEnabled(enabled: Boolean)
    suspend fun setPlayerLongPressSpeedEnabled(enabled: Boolean)
    suspend fun setPlayerAutoplayEnabled(enabled: Boolean)
    suspend fun setPlayerAutoplayCountdownSeconds(seconds: Int)
    suspend fun setPlayerPauseInBackground(enabled: Boolean)
}
