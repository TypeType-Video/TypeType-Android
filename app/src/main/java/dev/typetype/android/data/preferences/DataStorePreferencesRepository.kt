package dev.typetype.android.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
        )
    }

    override suspend fun setAccentColor(accentColor: AccentColor) {
        dataStore.edit { it[KEY_ACCENT_COLOR] = accentColor.name }
    }

    private companion object {
        val KEY_ACCENT_COLOR = stringPreferencesKey("accent_color")
    }
}
