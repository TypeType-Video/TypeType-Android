package dev.typetype.android.data.network

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "typetype_secrets",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun setAccessToken(token: String?) {
        prefs.edit().apply {
            if (token == null) remove(KEY_ACCESS_TOKEN) else putString(KEY_ACCESS_TOKEN, token)
        }.apply()
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
    }
}
