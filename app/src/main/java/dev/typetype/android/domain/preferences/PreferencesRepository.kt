package dev.typetype.android.domain.preferences

import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    fun observe(): Flow<AppPreferences>
    suspend fun setAccentColor(accentColor: AccentColor)
}
