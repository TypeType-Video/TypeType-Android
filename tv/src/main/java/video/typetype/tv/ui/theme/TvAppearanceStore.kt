package video.typetype.tv.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appearanceDataStore by preferencesDataStore(name = "tv_appearance")

public class TvAppearanceStore(context: Context) {
    private val store = context.applicationContext.appearanceDataStore

    public val appearance: Flow<TvAppearance> = store.data.map { values ->
        TvAppearance(
            personality = values[PERSONALITY].enumOr(TvPersonality.Classic),
            colorTheme = values[COLOR_THEME].enumOr(TvColorTheme.TypeType),
            colorMode = values[COLOR_MODE].enumOr(TvColorMode.Dark),
            amoled = values[AMOLED] ?: false,
            mangaPaper = values[MANGA_PAPER].enumOr(TvMangaPaper.Day),
            headlineMarker = values[HEADLINE_MARKER].enumOr(TvHeadlineMarker.Stamp),
            screentone = values[SCREENTONE] ?: true,
            speedLines = values[SPEED_LINES] ?: true,
            starburst = values[STARBURST] ?: true,
            inkedIcons = values[INKED_ICONS] ?: true,
            panelTilt = values[PANEL_TILT] ?: false,
            motion = values[MOTION].enumOr(TvMotion.Subtle),
        )
    }

    public suspend fun save(value: TvAppearance) {
        store.edit {
            it[PERSONALITY] = value.personality.name
            it[COLOR_THEME] = value.colorTheme.name
            it[COLOR_MODE] = value.colorMode.name
            it[AMOLED] = value.amoled
            it[MANGA_PAPER] = value.mangaPaper.name
            it[HEADLINE_MARKER] = value.headlineMarker.name
            it[SCREENTONE] = value.screentone
            it[SPEED_LINES] = value.speedLines
            it[STARBURST] = value.starburst
            it[INKED_ICONS] = value.inkedIcons
            it[PANEL_TILT] = value.panelTilt
            it[MOTION] = value.motion.name
        }
    }

    private companion object {
        val PERSONALITY = stringPreferencesKey("personality")
        val COLOR_THEME = stringPreferencesKey("color_theme")
        val COLOR_MODE = stringPreferencesKey("color_mode")
        val AMOLED = booleanPreferencesKey("amoled")
        val MANGA_PAPER = stringPreferencesKey("manga_paper")
        val HEADLINE_MARKER = stringPreferencesKey("headline_marker")
        val SCREENTONE = booleanPreferencesKey("screentone")
        val SPEED_LINES = booleanPreferencesKey("speed_lines")
        val STARBURST = booleanPreferencesKey("starburst")
        val INKED_ICONS = booleanPreferencesKey("inked_icons")
        val PANEL_TILT = booleanPreferencesKey("panel_tilt")
        val MOTION = stringPreferencesKey("motion")
    }
}

private inline fun <reified T : Enum<T>> String?.enumOr(fallback: T): T =
    enumValues<T>().firstOrNull { it.name == this } ?: fallback
