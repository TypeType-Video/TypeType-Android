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

    fun <T> createLiveChat(
        baseUrl: String,
        serverId: String,
        accountId: String,
        token: String,
        type: Class<T>,
    ): T {
        val client = scopedHttpClientFactory.create(baseUrl, serverId, accountId, token).liveChatClient()
        return retrofitFactory.createWithClient(baseUrl, type, client)
    }

    fun <T> createYoutubeTakeoutImport(
        baseUrl: String,
        serverId: String,
        accountId: String,
        token: String,
        type: Class<T>,
    ): T {
        val client = scopedHttpClientFactory.create(baseUrl, serverId, accountId, token)
            .youtubeTakeoutImportClient()
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

// The chat is an open event stream: no total call limit, and the server keep-alives arrive well within the read limit.
internal fun okhttp3.OkHttpClient.liveChatClient(): okhttp3.OkHttpClient = newBuilder()
    .retryOnConnectionFailure(false)
    .readTimeout(LIVE_CHAT_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .callTimeout(0, TimeUnit.SECONDS)
    .build()

private const val LIVE_CHAT_READ_TIMEOUT_SECONDS = 60L

internal fun okhttp3.OkHttpClient.youtubeTakeoutImportClient(): okhttp3.OkHttpClient = newBuilder()
    .followRedirects(false)
    .followSslRedirects(false)
    .retryOnConnectionFailure(false)
    .writeTimeout(YOUTUBE_TAKEOUT_WRITE_TIMEOUT_MINUTES, TimeUnit.MINUTES)
    .readTimeout(YOUTUBE_TAKEOUT_READ_TIMEOUT_MINUTES, TimeUnit.MINUTES)
    .callTimeout(YOUTUBE_TAKEOUT_CALL_TIMEOUT_HOURS, TimeUnit.HOURS)
    .build()

private const val YOUTUBE_TAKEOUT_WRITE_TIMEOUT_MINUTES = 2L
private const val YOUTUBE_TAKEOUT_READ_TIMEOUT_MINUTES = 15L
private const val YOUTUBE_TAKEOUT_CALL_TIMEOUT_HOURS = 6L
