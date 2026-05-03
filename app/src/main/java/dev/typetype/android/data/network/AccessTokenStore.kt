package dev.typetype.android.data.network

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val aead: Aead = run {
        AeadConfig.register()
        AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREFS_NAME)
            .withKeyTemplate(KeyTemplates.get(KEY_TEMPLATE))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    fun getAccessToken(): String? {
        val encoded = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        return runCatching {
            val ciphertext = Base64.decode(encoded, Base64.NO_WRAP)
            String(aead.decrypt(ciphertext, ASSOCIATED_DATA), Charsets.UTF_8)
        }.getOrNull()
    }

    fun setAccessToken(token: String?) {
        prefs.edit().apply {
            if (token == null) {
                remove(KEY_ACCESS_TOKEN)
            } else {
                val ciphertext = aead.encrypt(token.toByteArray(Charsets.UTF_8), ASSOCIATED_DATA)
                putString(KEY_ACCESS_TOKEN, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            }
        }.apply()
    }

    private companion object {
        const val PREFS_NAME = "typetype_secrets"
        const val KEYSET_NAME = "typetype_token_keyset"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val MASTER_KEY_URI = "android-keystore://typetype_master_key"
        const val KEY_TEMPLATE = "AES256_GCM"
        val ASSOCIATED_DATA = "typetype.android".toByteArray(Charsets.UTF_8)
    }
}
