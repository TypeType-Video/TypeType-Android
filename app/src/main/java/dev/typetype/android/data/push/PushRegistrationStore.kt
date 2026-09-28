package dev.typetype.android.data.push

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.typetype.android.data.account.AccountScope
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class PushRegistrationStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    data class Registration(
        val deviceId: String,
        val endpoint: String?,
        val p256dh: String?,
        val auth: String?,
    )

    fun registration(scope: AccountScope): Flow<Registration?> = dataStore.data.map { prefs ->
        prefs.registration(scope)
    }

    suspend fun registrationOnce(scope: AccountScope): Registration? =
        dataStore.data.first().registration(scope)

    suspend fun ensureDeviceId(scope: AccountScope): String {
        dataStore.data.first()[deviceIdKey(scope)]?.takeIf(String::isNotBlank)?.let { return it }
        val deviceId = UUID.randomUUID().toString()
        dataStore.edit { prefs ->
            if (prefs[deviceIdKey(scope)].isNullOrBlank()) {
                prefs[deviceIdKey(scope)] = deviceId
            }
        }
        return dataStore.data.first()[deviceIdKey(scope)] ?: error("The stored push device id disappeared")
    }

    suspend fun setSubscription(scope: AccountScope, endpoint: String, p256dh: String, auth: String) {
        dataStore.edit { prefs ->
            prefs[endpointKey(scope)] = endpoint
            prefs[p256dhKey(scope)] = p256dh
            prefs[authKey(scope)] = auth
        }
    }

    suspend fun clear(scope: AccountScope) {
        dataStore.edit { prefs ->
            prefs.remove(deviceIdKey(scope))
            prefs.remove(endpointKey(scope))
            prefs.remove(p256dhKey(scope))
            prefs.remove(authKey(scope))
        }
    }

    private fun Preferences.registration(scope: AccountScope): Registration? {
        val deviceId = this[deviceIdKey(scope)]?.takeIf(String::isNotBlank) ?: return null
        return Registration(
            deviceId = deviceId,
            endpoint = this[endpointKey(scope)],
            p256dh = this[p256dhKey(scope)],
            auth = this[authKey(scope)],
        )
    }

    private fun deviceIdKey(scope: AccountScope) =
        stringPreferencesKey("push_device_id_" + scopeKey(scope))

    private fun endpointKey(scope: AccountScope) =
        stringPreferencesKey("push_endpoint_" + scopeKey(scope))

    private fun p256dhKey(scope: AccountScope) =
        stringPreferencesKey("push_p256dh_" + scopeKey(scope))

    private fun authKey(scope: AccountScope) =
        stringPreferencesKey("push_auth_" + scopeKey(scope))

    private fun scopeKey(scope: AccountScope) = "${scope.serverId}_${scope.accountId}"
}
