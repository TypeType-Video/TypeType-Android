package dev.typetype.android.data.session

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

@Singleton
class DeviceIdentityStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getOrCreate(): String = synchronized(preferences) {
        preferences.getString(DEVICE_ID_KEY, null)
            ?.takeIf { runCatching { UUID.fromString(it) }.isSuccess }
            ?: UUID.randomUUID().toString().also { created ->
                preferences.edit(commit = true) { putString(DEVICE_ID_KEY, created) }
            }
    }

    fun getDeviceName(): String = preferences.getString(DEVICE_NAME_KEY, null)
        ?.take(MAX_DEVICE_NAME_LENGTH)
        ?: DEFAULT_DEVICE_NAME

    fun observeDeviceName(): Flow<String> = callbackFlow {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == DEVICE_NAME_KEY) trySend(getDeviceName())
        }
        trySend(getDeviceName())
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun setDeviceName(name: String) {
        val normalized = name.take(MAX_DEVICE_NAME_LENGTH)
        preferences.edit { putString(DEVICE_NAME_KEY, normalized) }
    }

    private companion object {
        const val PREFERENCES_NAME = "typetype_device"
        const val DEVICE_ID_KEY = "installation_id"
        const val DEVICE_NAME_KEY = "device_name"
        const val DEFAULT_DEVICE_NAME = "Android device"
        const val MAX_DEVICE_NAME_LENGTH = 120
    }
}
