package dev.typetype.android

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.typetype.android.app.BuildConfig
import dev.typetype.android.domain.auth.OidcRedirect
import dev.typetype.android.domain.version.AppVersion
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppInfoModule {
    @Provides
    @Singleton
    fun oidcRedirect(): OidcRedirect = OidcRedirect(BuildConfig.APPLICATION_ID)

    @Provides
    @Singleton
    fun appVersion(): AppVersion = AppVersion(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toLong())
}
