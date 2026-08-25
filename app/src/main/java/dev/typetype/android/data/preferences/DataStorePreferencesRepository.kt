package dev.typetype.android.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.typetype.android.domain.preferences.AccentColor
import dev.typetype.android.domain.preferences.AppearanceFont
import dev.typetype.android.domain.preferences.AppearanceMode
import dev.typetype.android.domain.preferences.AppearanceMotion
import dev.typetype.android.domain.preferences.AppearancePersonality
import dev.typetype.android.domain.preferences.AppPreferences
import dev.typetype.android.domain.preferences.MangaHeadlineMarker
import dev.typetype.android.domain.preferences.MangaPaper
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
            appearancePersonality = prefs.enum(KEY_APPEARANCE_PERSONALITY, AppearancePersonality.Classic),
            appearanceMode = prefs.enum(KEY_APPEARANCE_MODE, AppearanceMode.System),
            appearanceAmoled = prefs[KEY_APPEARANCE_AMOLED] ?: false,
            appearanceFont = prefs.enum(KEY_APPEARANCE_FONT, AppearanceFont.System),
            appearanceMotion = prefs.enum(KEY_APPEARANCE_MOTION, AppearanceMotion.Subtle),
            mangaPaper = prefs.enum(KEY_MANGA_PAPER, MangaPaper.Day),
            mangaHeadlineMarker = prefs.enum(KEY_MANGA_HEADLINE_MARKER, MangaHeadlineMarker.Stamp),
            mangaScreentone = prefs[KEY_MANGA_SCREENTONE] ?: true,
            mangaSpeedLines = prefs[KEY_MANGA_SPEED_LINES] ?: true,
            mangaStarburst = prefs[KEY_MANGA_STARBURST] ?: true,
            mangaInkedIcons = prefs[KEY_MANGA_INKED_ICONS] ?: true,
            mangaPanelTilt = prefs[KEY_MANGA_PANEL_TILT] ?: false,
            playerDoubleTapSeekEnabled = prefs[KEY_PLAYER_DOUBLE_TAP_SEEK] ?: true,
            playerDoubleTapSeekSeconds = (prefs[KEY_PLAYER_DOUBLE_TAP_SEEK_SECONDS] ?: 10)
                .takeIf(ALLOWED_DOUBLE_TAP_SEEK_SECONDS::contains) ?: 10,
            playerPreferredCodec = prefs[KEY_PLAYER_PREFERRED_CODEC]
                ?.takeIf(ALLOWED_PLAYER_CODECS::contains) ?: "recommended",
            playerSwipeSeekEnabled = prefs[KEY_PLAYER_SWIPE_SEEK] ?: true,
            playerSwipeBrightnessVolumeEnabled = prefs[KEY_PLAYER_SWIPE_BRIGHT_VOL] ?: true,
            playerPlaybackBrightnessPercent = prefs[KEY_PLAYER_PLAYBACK_BRIGHTNESS]
                ?.coerceIn(0, 100),
            playerLongPressSpeedEnabled = prefs[KEY_PLAYER_LONG_PRESS_SPEED] ?: true,
            playerAccessibleControlsEnabled = prefs[KEY_PLAYER_ACCESSIBLE_CONTROLS] ?: false,
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

    override suspend fun setAppearancePersonality(personality: AppearancePersonality) =
        store(KEY_APPEARANCE_PERSONALITY, personality.name)

    override suspend fun setAppearanceMode(mode: AppearanceMode) = store(KEY_APPEARANCE_MODE, mode.name)

    override suspend fun setAppearanceAmoled(enabled: Boolean) = store(KEY_APPEARANCE_AMOLED, enabled)

    override suspend fun setAppearanceFont(font: AppearanceFont) = store(KEY_APPEARANCE_FONT, font.name)

    override suspend fun setAppearanceMotion(motion: AppearanceMotion) = store(KEY_APPEARANCE_MOTION, motion.name)

    override suspend fun setMangaPaper(paper: MangaPaper) = store(KEY_MANGA_PAPER, paper.name)

    override suspend fun setMangaHeadlineMarker(marker: MangaHeadlineMarker) =
        store(KEY_MANGA_HEADLINE_MARKER, marker.name)

    override suspend fun setMangaScreentone(enabled: Boolean) = store(KEY_MANGA_SCREENTONE, enabled)

    override suspend fun setMangaSpeedLines(enabled: Boolean) = store(KEY_MANGA_SPEED_LINES, enabled)

    override suspend fun setMangaStarburst(enabled: Boolean) = store(KEY_MANGA_STARBURST, enabled)

    override suspend fun setMangaInkedIcons(enabled: Boolean) = store(KEY_MANGA_INKED_ICONS, enabled)

    override suspend fun setMangaPanelTilt(enabled: Boolean) = store(KEY_MANGA_PANEL_TILT, enabled)

    override suspend fun setPlayerDoubleTapSeekEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_PLAYER_DOUBLE_TAP_SEEK] = enabled }
    }

    override suspend fun setPlayerDoubleTapSeekSeconds(seconds: Int) {
        val value = seconds.takeIf(ALLOWED_DOUBLE_TAP_SEEK_SECONDS::contains) ?: 10
        dataStore.edit { it[KEY_PLAYER_DOUBLE_TAP_SEEK_SECONDS] = value }
    }

    override suspend fun setPlayerPreferredCodec(codec: String) {
        val value = codec.takeIf(ALLOWED_PLAYER_CODECS::contains) ?: "recommended"
        dataStore.edit { it[KEY_PLAYER_PREFERRED_CODEC] = value }
    }

    override suspend fun setPlayerSwipeSeekEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_PLAYER_SWIPE_SEEK] = enabled }
    }

    override suspend fun setPlayerSwipeBrightnessVolumeEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_PLAYER_SWIPE_BRIGHT_VOL] = enabled }
    }

    override suspend fun setPlayerPlaybackBrightnessPercent(percent: Int) {
        dataStore.edit { it[KEY_PLAYER_PLAYBACK_BRIGHTNESS] = percent.coerceIn(0, 100) }
    }

    override suspend fun setPlayerLongPressSpeedEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_PLAYER_LONG_PRESS_SPEED] = enabled }
    }

    override suspend fun setPlayerAccessibleControlsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_PLAYER_ACCESSIBLE_CONTROLS] = enabled }
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

    private suspend fun store(key: Preferences.Key<String>, value: String) {
        dataStore.edit { it[key] = value }
    }

    private suspend fun store(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { it[key] = value }
    }

    private companion object {
        val KEY_ACCENT_COLOR = stringPreferencesKey("accent_color")
        val KEY_APPEARANCE_PERSONALITY = stringPreferencesKey("appearance_personality")
        val KEY_APPEARANCE_MODE = stringPreferencesKey("appearance_mode")
        val KEY_APPEARANCE_AMOLED = booleanPreferencesKey("appearance_amoled")
        val KEY_APPEARANCE_FONT = stringPreferencesKey("appearance_font")
        val KEY_APPEARANCE_MOTION = stringPreferencesKey("appearance_motion")
        val KEY_MANGA_PAPER = stringPreferencesKey("manga_paper")
        val KEY_MANGA_HEADLINE_MARKER = stringPreferencesKey("manga_headline_marker")
        val KEY_MANGA_SCREENTONE = booleanPreferencesKey("manga_screentone")
        val KEY_MANGA_SPEED_LINES = booleanPreferencesKey("manga_speed_lines")
        val KEY_MANGA_STARBURST = booleanPreferencesKey("manga_starburst")
        val KEY_MANGA_INKED_ICONS = booleanPreferencesKey("manga_inked_icons")
        val KEY_MANGA_PANEL_TILT = booleanPreferencesKey("manga_panel_tilt")
        val KEY_PLAYER_DOUBLE_TAP_SEEK = booleanPreferencesKey("player_double_tap_seek")
        val KEY_PLAYER_DOUBLE_TAP_SEEK_SECONDS = intPreferencesKey("player_double_tap_seek_seconds")
        val KEY_PLAYER_PREFERRED_CODEC = stringPreferencesKey("player_preferred_codec")
        val KEY_PLAYER_SWIPE_SEEK = booleanPreferencesKey("player_swipe_seek")
        val KEY_PLAYER_SWIPE_BRIGHT_VOL = booleanPreferencesKey("player_swipe_bright_vol")
        val KEY_PLAYER_PLAYBACK_BRIGHTNESS = intPreferencesKey("player_playback_brightness")
        val KEY_PLAYER_LONG_PRESS_SPEED = booleanPreferencesKey("player_long_press_speed")
        val KEY_PLAYER_ACCESSIBLE_CONTROLS = booleanPreferencesKey("player_accessible_controls")
        val KEY_PLAYER_AUTOPLAY = booleanPreferencesKey("player_autoplay")
        val KEY_PLAYER_AUTOPLAY_COUNTDOWN = intPreferencesKey("player_autoplay_countdown_seconds")
        val KEY_PLAYER_AUDIO_ONLY_PLAYBACK = booleanPreferencesKey("player_audio_only_playback")
        val KEY_PLAYER_PAUSE_BACKGROUND = booleanPreferencesKey("player_pause_background")
        val KEY_DANMAKU_ENABLED = booleanPreferencesKey("player_danmaku_enabled")
        val KEY_DANMAKU_SPEED = floatPreferencesKey("player_danmaku_speed")
        val KEY_DANMAKU_SIZE = floatPreferencesKey("player_danmaku_size")
        const val DEFAULT_AUTOPLAY_COUNTDOWN_SECONDS = 10
        const val MAX_AUTOPLAY_COUNTDOWN_SECONDS = 60
        val ALLOWED_DOUBLE_TAP_SEEK_SECONDS = setOf(5, 10, 15, 20, 30)
        val ALLOWED_PLAYER_CODECS = setOf("recommended", "av1", "vp9", "h264")
    }
}

private inline fun <reified T : Enum<T>> Preferences.enum(
    key: Preferences.Key<String>,
    fallback: T,
): T = this[key]?.let { stored -> enumValues<T>().firstOrNull { it.name == stored } } ?: fallback
