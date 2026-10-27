package dev.typetype.android.data.network

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.core.content.edit
import dev.typetype.android.data.account.AccountScopeStore
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessTokenStore @Inject constructor(
    @ApplicationContext context: Context,
    private val accountScopeStore: AccountScopeStore,
) : ScopedAccessTokenStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val aead: Aead by lazy {
        AeadConfig.register()
        AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREFS_NAME)
            .withKeyTemplate(KeyTemplates.get(KEY_TEMPLATE))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    @Synchronized
    fun getAccessToken(serverId: String): String? {
        val accountId = accountScopeStore.getCurrentAccountId(serverId) ?: PENDING_ACCOUNT_ID
        return readToken(serverId, accountId)
            ?: migrateServerToken(serverId, accountId)
            ?: migrateLegacyToken(serverId, accountId)
    }

    @Synchronized
    override fun getAccessToken(serverId: String, accountId: String): String? =
        readToken(serverId, accountId)

    @Synchronized
    fun hasAccessToken(serverId: String, accountId: String): Boolean =
        prefs.contains(tokenKey(serverId, accountId)) ||
            prefs.contains(serverTokenKey(serverId)) ||
            prefs.contains(KEY_ACCESS_TOKEN)

    @Synchronized
    fun setAccessToken(serverId: String, token: String?) {
        val accountId = accountScopeStore.getCurrentAccountId(serverId) ?: PENDING_ACCOUNT_ID
        writeToken(serverId, accountId, token)
    }

    @Synchronized
    override fun setAccessToken(serverId: String, accountId: String, token: String?) {
        writeToken(serverId, accountId, token)
    }

    @Synchronized
    fun setAuthenticatedAccessToken(serverId: String, accountId: String, token: String) {
        writeToken(serverId, accountId, token)
        accountScopeStore.setCurrentAccountId(serverId, accountId)
        writeToken(serverId, PENDING_ACCOUNT_ID, null)
    }

    @Synchronized
    fun removeAccount(serverId: String, accountId: String) {
        writeToken(serverId, accountId, null)
        if (accountScopeStore.getCurrentAccountId(serverId) == accountId) {
            accountScopeStore.clearCurrentAccountId(serverId)
        }
    }

    private fun readToken(serverId: String, accountId: String): String? {
        val encoded = prefs.getString(tokenKey(serverId, accountId), null) ?: return null
        return runCatching {
            val ciphertext = Base64.decode(encoded, Base64.NO_WRAP)
            String(aead.decrypt(ciphertext, associatedData(serverId, accountId)), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun writeToken(serverId: String, accountId: String, token: String?) {
        val key = tokenKey(serverId, accountId)
        prefs.edit {
            if (token == null) {
                remove(key)
            } else {
                val ciphertext = aead.encrypt(
                    token.toByteArray(Charsets.UTF_8),
                    associatedData(serverId, accountId),
                )
                putString(key, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            }
        }
    }

    private fun migrateServerToken(serverId: String, accountId: String): String? {
        val oldKey = serverTokenKey(serverId)
        val encoded = prefs.getString(oldKey, null) ?: return null
        val token = decrypt(encoded, "typetype.android:$serverId".toByteArray(Charsets.UTF_8))
        prefs.edit { remove(oldKey) }
        if (token != null) writeToken(serverId, accountId, token)
        return token
    }

    private fun migrateLegacyToken(serverId: String, accountId: String): String? {
        val legacy = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val token = decrypt(legacy, LEGACY_ASSOCIATED_DATA)
        prefs.edit { remove(KEY_ACCESS_TOKEN) }
        if (token != null) writeToken(serverId, accountId, token)
        return token
    }

    private fun decrypt(encoded: String, associatedData: ByteArray): String? = runCatching {
        val ciphertext = Base64.decode(encoded, Base64.NO_WRAP)
        String(aead.decrypt(ciphertext, associatedData), Charsets.UTF_8)
    }.getOrNull()

    private fun tokenKey(serverId: String, accountId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$serverId\u0000$accountId".toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }
        return "access_token_$digest"
    }

    private fun serverTokenKey(serverId: String): String = "access_token_$serverId"

    private fun associatedData(serverId: String, accountId: String): ByteArray =
        "typetype.android:$serverId:$accountId".toByteArray(Charsets.UTF_8)

    private companion object {
        const val PREFS_NAME = "typetype_secrets"
        const val KEYSET_NAME = "typetype_token_keyset"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val MASTER_KEY_URI = "android-keystore://typetype_master_key"
        const val KEY_TEMPLATE = "AES256_GCM"
        const val PENDING_ACCOUNT_ID = "__pending__"
        val LEGACY_ASSOCIATED_DATA = "typetype.android".toByteArray(Charsets.UTF_8)
    }
}
