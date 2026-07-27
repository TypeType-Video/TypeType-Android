package dev.typetype.android.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.typetype.android.data.diagnostics.DiagnosticsInterceptor
import dev.typetype.android.data.network.PersistentCookieJar
import dev.typetype.android.data.network.UserAgentInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    @Provides
    @Singleton
    @Named("refresh")
    fun provideRefreshOkHttpClient(
        userAgent: UserAgentInterceptor,
        cookieJar: PersistentCookieJar,
        diagnostics: DiagnosticsInterceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .cookieJar(cookieJar)
            .addInterceptor(userAgent)
            .addInterceptor(diagnostics)
            .build()
    }

}
