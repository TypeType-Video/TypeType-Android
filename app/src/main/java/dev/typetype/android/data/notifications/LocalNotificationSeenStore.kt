package dev.typetype.android.data.notifications

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.typetype.android.data.account.AccountScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class LocalNotificationSeenStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    data class State(
        val seeded: Boolean,
        val keys: List<String>,
    )

    suspend fun state(scope: AccountScope): State = dataStore.data.first().toState(scope)

    suspend fun record(scope: AccountScope, keys: List<String>, seeded: Boolean) {
        if (keys.isEmpty() && !seeded) return
        dataStore.edit { preferences ->
            val merged = (preferences.toState(scope).keys + keys).distinct().takeLast(MAX_KEYS)
            preferences[keysKey(scope)] = merged.joinToString(SEPARATOR)
            preferences[seededKey(scope)] = seeded
        }
    }

    suspend fun clear(scope: AccountScope) {
        dataStore.edit { preferences ->
            preferences.remove(keysKey(scope))
            preferences.remove(seededKey(scope))
        }
    }

    private fun Preferences.toState(scope: AccountScope): State = State(
        seeded = this[seededKey(scope)] ?: false,
        keys = this[keysKey(scope)]
            ?.split(SEPARATOR)
            ?.filter { it.isNotBlank() }
            .orEmpty(),
    )

    private fun keysKey(scope: AccountScope) =
        stringPreferencesKey("local_notifications_${scope.serverId}_${scope.accountId}_seen")

    private fun seededKey(scope: AccountScope) =
        booleanPreferencesKey("local_notifications_${scope.serverId}_${scope.accountId}_seeded")

    private companion object {
        const val SEPARATOR = "\n"
        const val MAX_KEYS = 300
    }
}
