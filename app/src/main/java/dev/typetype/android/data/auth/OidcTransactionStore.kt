package dev.typetype.android.data.auth

import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.core.content.edit
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OidcTransactionStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun start(serverId: String, state: String) {
        val stored = preferences.edit()
            .putString(KEY_SERVER_ID, serverId)
            .putString(KEY_STATE_HASH, stateHash(state))
            .putLong(KEY_STARTED_AT, System.currentTimeMillis())
            .commit()
        check(stored) { "Could not save the OIDC transaction" }
    }

    fun requireMatches(serverId: String, state: String) {
        val expectedServerId = preferences.getString(KEY_SERVER_ID, null)
        val expectedHash = preferences.getString(KEY_STATE_HASH, null)
        val startedAt = preferences.getLong(KEY_STARTED_AT, 0L)
        check(expectedServerId == serverId && expectedHash != null && startedAt > 0L) {
            "No OIDC sign-in is waiting for this callback"
        }
        check(System.currentTimeMillis() - startedAt <= MAX_AGE_MILLIS) {
            "The OIDC sign-in has expired, please try again"
        }
        check(MessageDigest.isEqual(expectedHash.toByteArray(), stateHash(state).toByteArray())) {
            "The OIDC callback does not match the current sign-in"
        }
    }

    fun clear(serverId: String) {
        if (preferences.getString(KEY_SERVER_ID, null) == serverId) {
            preferences.edit { clear() }
        }
    }

    private fun stateHash(state: String): String = Base64.encodeToString(
        MessageDigest.getInstance("SHA-256").digest(state.toByteArray(Charsets.UTF_8)),
        Base64.NO_WRAP,
    )

    private companion object {
        const val PREFERENCES_NAME = "typetype_oidc_transaction"
        const val KEY_SERVER_ID = "server_id"
        const val KEY_STATE_HASH = "state_hash"
        const val KEY_STARTED_AT = "started_at"
        const val MAX_AGE_MILLIS = 12 * 60 * 1_000L
    }
}
