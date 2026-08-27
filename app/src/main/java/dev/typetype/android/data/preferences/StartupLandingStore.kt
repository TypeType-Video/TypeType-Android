package dev.typetype.android.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.typetype.android.data.account.AccountScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
class StartupLandingStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    fun observeLandingPage(scope: AccountScope): Flow<String> = dataStore.data.map { prefs ->
        prefs[landingKey(scope)].orEmpty()
    }.distinctUntilChanged()

    suspend fun setLandingPage(scope: AccountScope, landingPage: String) {
        dataStore.edit { prefs ->
            val key = landingKey(scope)
            if (landingPage.isBlank()) prefs.remove(key) else prefs[key] = landingPage
        }
    }

    private fun landingKey(scope: AccountScope) =
        stringPreferencesKey("startup_landing_${scope.serverId}_${scope.accountId}")
}
