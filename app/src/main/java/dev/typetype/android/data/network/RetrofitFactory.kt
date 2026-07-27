package dev.typetype.android.data.network

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.CookieJar
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Singleton
class RetrofitFactory @Inject constructor(
    @param:javax.inject.Named("refresh") private val sessionClient: OkHttpClient,
    private val json: Json,
) {
    private val explicitTokenClient = sessionClient.newBuilder()
        .cookieJar(CookieJar.NO_COOKIES)
        .build()

    fun create(baseUrl: String): TypeTypeApi = create(baseUrl, TypeTypeApi::class.java)

    fun <T> create(baseUrl: String, type: Class<T>): T {
        return createWithClient(baseUrl, type, explicitTokenClient)
    }

    fun createWithoutAutomaticAuthentication(baseUrl: String): TypeTypeApi =
        createWithClient(baseUrl, TypeTypeApi::class.java, sessionClient)

    fun createForExplicitToken(baseUrl: String): TypeTypeApi =
        createWithClient(baseUrl, TypeTypeApi::class.java, explicitTokenClient)

    fun <T> createForExplicitToken(baseUrl: String, type: Class<T>): T =
        createWithClient(baseUrl, type, explicitTokenClient)

    internal fun <T> createWithClient(baseUrl: String, type: Class<T>, client: OkHttpClient): T {
        val normalized = if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(type)
    }
}
