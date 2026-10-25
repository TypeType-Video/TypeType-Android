package dev.typetype.android.data.appupdate

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.domain.appupdate.AppUpdateChannel
import dev.typetype.android.domain.appupdate.AppUpdateSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DataStoreAppUpdateSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context,
) : AppUpdateSettingsRepository {
    private val initialChannel = detectInitialChannel()

    override fun observeChannel(): Flow<AppUpdateChannel> = dataStore.data.map { preferences ->
        preferences[KEY_CHANNEL]
            ?.let { stored -> AppUpdateChannel.entries.firstOrNull { it.name == stored } }
            ?: initialChannel
    }

    override suspend fun setChannel(channel: AppUpdateChannel) {
        dataStore.edit { it[KEY_CHANNEL] = channel.name }
    }

    private fun detectInitialChannel(): AppUpdateChannel {
        val installer = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
        }.getOrNull()
        return if (installer in F_DROID_INSTALLERS) {
            AppUpdateChannel.F_DROID
        } else {
            AppUpdateChannel.GITHUB_STABLE
        }
    }

    private companion object {
        val KEY_CHANNEL = stringPreferencesKey("app_update_channel")
        val F_DROID_INSTALLERS = setOf("org.fdroid.fdroid", "org.fdroid.basic")
    }
}
