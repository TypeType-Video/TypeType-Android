package dev.typetype.android.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.typetype.android.BuildConfig
import dev.typetype.android.data.network.AuthInterceptor
import dev.typetype.android.data.network.PersistentCookieJar
import dev.typetype.android.data.network.TokenAuthenticator
import dev.typetype.android.data.network.UserAgentInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

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
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .cookieJar(cookieJar)
            .addInterceptor(userAgent)
            .addDebugLogging()
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        userAgent: UserAgentInterceptor,
        auth: AuthInterceptor,
        cookieJar: PersistentCookieJar,
        authenticator: TokenAuthenticator,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(75, TimeUnit.SECONDS)
            .cookieJar(cookieJar)
            .addInterceptor(userAgent)
            .addInterceptor(auth)
            .addDebugLogging()
            .authenticator(authenticator)
            .build()
    }

    private fun OkHttpClient.Builder.addDebugLogging(): OkHttpClient.Builder =
        if (BuildConfig.DEBUG) {
            addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        } else {
            this
        }
}
