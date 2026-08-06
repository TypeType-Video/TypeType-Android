package dev.typetype.android.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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
                ?: AccentColor.Blue,
            playerDoubleTapSeekEnabled = prefs[KEY_PLAYER_DOUBLE_TAP_SEEK] ?: true,
            playerSwipeSeekEnabled = prefs[KEY_PLAYER_SWIPE_SEEK] ?: true,
            playerSwipeBrightnessVolumeEnabled = prefs[KEY_PLAYER_SWIPE_BRIGHT_VOL] ?: true,
            playerLongPressSpeedEnabled = prefs[KEY_PLAYER_LONG_PRESS_SPEED] ?: true,
            playerAutoplayEnabled = prefs[KEY_PLAYER_AUTOPLAY] ?: true,
            playerAutoplayCountdownSeconds = (
                prefs[KEY_PLAYER_AUTOPLAY_COUNTDOWN] ?: DEFAULT_AUTOPLAY_COUNTDOWN_SECONDS
            ).coerceIn(0, MAX_AUTOPLAY_COUNTDOWN_SECONDS),
            playerAudioOnlyPlayback = prefs[KEY_PLAYER_AUDIO_ONLY_PLAYBACK] ?: false,
            playerPauseInBackground = prefs[KEY_PLAYER_PAUSE_BACKGROUND] ?: false,
            danmakuEnabled = prefs[KEY_DANMAKU_ENABLED] ?: false,
            danmakuSpeed = (prefs[KEY_DANMAKU_SPEED] ?: 1f).coerceIn(0.5f, 2f),
            danmakuSize = (prefs[KEY_DANMAKU_SIZE] ?: 1f).coerceIn(0.5f, 2f),
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

    override suspend fun setPlayerAutoplayCountdownSeconds(seconds: Int) {
        dataStore.edit {
            it[KEY_PLAYER_AUTOPLAY_COUNTDOWN] = seconds.coerceIn(
                0,
                MAX_AUTOPLAY_COUNTDOWN_SECONDS,
            )
        }
    }

    override suspend fun setPlayerAudioOnlyPlayback(enabled: Boolean) {
        dataStore.edit { it[KEY_PLAYER_AUDIO_ONLY_PLAYBACK] = enabled }
    }

    override suspend fun setPlayerPauseInBackground(enabled: Boolean) {
        dataStore.edit { it[KEY_PLAYER_PAUSE_BACKGROUND] = enabled }
    }

    override suspend fun setDanmakuEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_DANMAKU_ENABLED] = enabled }
    }

    override suspend fun setDanmakuSpeed(speed: Float) {
        dataStore.edit { it[KEY_DANMAKU_SPEED] = speed.coerceIn(0.5f, 2f) }
    }

    override suspend fun setDanmakuSize(size: Float) {
        dataStore.edit { it[KEY_DANMAKU_SIZE] = size.coerceIn(0.5f, 2f) }
    }

    private companion object {
        val KEY_ACCENT_COLOR = stringPreferencesKey("accent_color")
        val KEY_PLAYER_DOUBLE_TAP_SEEK = booleanPreferencesKey("player_double_tap_seek")
        val KEY_PLAYER_SWIPE_SEEK = booleanPreferencesKey("player_swipe_seek")
        val KEY_PLAYER_SWIPE_BRIGHT_VOL = booleanPreferencesKey("player_swipe_bright_vol")
        val KEY_PLAYER_LONG_PRESS_SPEED = booleanPreferencesKey("player_long_press_speed")
        val KEY_PLAYER_AUTOPLAY = booleanPreferencesKey("player_autoplay")
        val KEY_PLAYER_AUTOPLAY_COUNTDOWN = intPreferencesKey("player_autoplay_countdown_seconds")
        val KEY_PLAYER_AUDIO_ONLY_PLAYBACK = booleanPreferencesKey("player_audio_only_playback")
        val KEY_PLAYER_PAUSE_BACKGROUND = booleanPreferencesKey("player_pause_background")
        val KEY_DANMAKU_ENABLED = booleanPreferencesKey("player_danmaku_enabled")
        val KEY_DANMAKU_SPEED = floatPreferencesKey("player_danmaku_speed")
        val KEY_DANMAKU_SIZE = floatPreferencesKey("player_danmaku_size")
        const val DEFAULT_AUTOPLAY_COUNTDOWN_SECONDS = 10
        const val MAX_AUTOPLAY_COUNTDOWN_SECONDS = 60
    }
}
