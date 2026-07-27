package dev.typetype.android.data.account

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

@Singleton
class AccountScopeStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getCurrentAccountId(serverId: String): String? =
        preferences.getString(key(serverId), null)

    fun observeCurrentAccountId(serverId: String): Flow<String?> = callbackFlow {
        val preferenceKey = key(serverId)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == preferenceKey) trySend(getCurrentAccountId(serverId))
        }
        trySend(getCurrentAccountId(serverId))
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun setCurrentAccountId(serverId: String, accountId: String) {
        preferences.edit { putString(key(serverId), accountId) }
    }

    fun clearCurrentAccountId(serverId: String) {
        preferences.edit { remove(key(serverId)) }
    }

    private fun key(serverId: String): String = "current_account_$serverId"

    private companion object {
        const val PREFERENCES_NAME = "typetype_account_scopes"
    }
}
