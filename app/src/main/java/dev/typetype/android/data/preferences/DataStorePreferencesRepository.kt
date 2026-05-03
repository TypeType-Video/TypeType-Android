package dev.typetype.android.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.typetype.android.domain.preferences.AccentColor
import dev.typetype.android.domain.preferences.AppPreferences
import dev.typetype.android.domain.preferences.PreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DataStorePreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : PreferencesRepository {

    override fun observe(): Flow<AppPreferences> = dataStore.data.map { prefs ->
        AppPreferences(
            accentColor = prefs[KEY_ACCENT_COLOR]
                ?.let { runCatching { AccentColor.valueOf(it) }.getOrNull() }
                ?: AccentColor.Red,
            playerDoubleTapSeekEnabled = prefs[KEY_PLAYER_DOUBLE_TAP_SEEK] ?: true,
            playerSwipeSeekEnabled = prefs[KEY_PLAYER_SWIPE_SEEK] ?: true,
            playerSwipeBrightnessVolumeEnabled = prefs[KEY_PLAYER_SWIPE_BRIGHT_VOL] ?: true,
            playerLongPressSpeedEnabled = prefs[KEY_PLAYER_LONG_PRESS_SPEED] ?: true,
            playerAutoplayEnabled = prefs[KEY_PLAYER_AUTOPLAY] ?: true,
            playerPauseInBackground = prefs[KEY_PLAYER_PAUSE_BACKGROUND] ?: false,
        )
    }

    override suspend fun setAccentColor(accentColor: AccentColor) {
        dataStore.edit { it[KEY_ACCENT_COLOR] = accentColor.name }
    }

    override suspend fun setPlayerDoubleTapSeekEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_PLAYER_DOUBLE_TAP_SEEK] = enabled }
    }

    override suspend fun setPlayerSwipeSeekEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_PLAYER_SWIPE_SEEK] = enabled }
    }

    override suspend fun setPlayerSwipeBrightnessVolumeEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_PLAYER_SWIPE_BRIGHT_VOL] = enabled }
    }

    override suspend fun setPlayerLongPressSpeedEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_PLAYER_LONG_PRESS_SPEED] = enabled }
    }

    override suspend fun setPlayerAutoplayEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_PLAYER_AUTOPLAY] = enabled }
    }

    override suspend fun setPlayerPauseInBackground(enabled: Boolean) {
        dataStore.edit { it[KEY_PLAYER_PAUSE_BACKGROUND] = enabled }
    }

    private companion object {
        val KEY_ACCENT_COLOR = stringPreferencesKey("accent_color")
        val KEY_PLAYER_DOUBLE_TAP_SEEK = booleanPreferencesKey("player_double_tap_seek")
        val KEY_PLAYER_SWIPE_SEEK = booleanPreferencesKey("player_swipe_seek")
        val KEY_PLAYER_SWIPE_BRIGHT_VOL = booleanPreferencesKey("player_swipe_bright_vol")
        val KEY_PLAYER_LONG_PRESS_SPEED = booleanPreferencesKey("player_long_press_speed")
        val KEY_PLAYER_AUTOPLAY = booleanPreferencesKey("player_autoplay")
        val KEY_PLAYER_PAUSE_BACKGROUND = booleanPreferencesKey("player_pause_background")
    }
}
