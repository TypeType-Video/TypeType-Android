package dev.typetype.android.data.network

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScopedApiFactory @Inject constructor(
    private val retrofitFactory: RetrofitFactory,
    private val scopedHttpClientFactory: ScopedHttpClientFactory,
) {
    fun <T> create(
        baseUrl: String,
        serverId: String,
        accountId: String,
        token: String,
        type: Class<T>,
    ): T {
        val client = scopedHttpClientFactory.create(baseUrl, serverId, accountId, token)
        return retrofitFactory.createWithClient(baseUrl, type, client)
    }

    fun <T> createSabr(
        baseUrl: String,
        serverId: String,
        accountId: String,
        token: String,
        type: Class<T>,
    ): T {
        val client = scopedHttpClientFactory.create(baseUrl, serverId, accountId, token)
            .sabrControlClient()
        return retrofitFactory.createWithClient(baseUrl, type, client)
    }
}

internal fun okhttp3.OkHttpClient.sabrControlClient(): okhttp3.OkHttpClient = newBuilder()
    .followRedirects(false)
    .followSslRedirects(false)
    .retryOnConnectionFailure(false)
    .addNetworkInterceptor(PlaybackRetryOwnershipInterceptor)
    .readTimeout(SABR_CONTROL_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .callTimeout(SABR_CONTROL_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .build()

private const val SABR_CONTROL_READ_TIMEOUT_SECONDS = 30L
private const val SABR_CONTROL_CALL_TIMEOUT_SECONDS = 45L
