package dev.typetype.android.data.network

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Singleton
class RetrofitFactory @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    fun create(baseUrl: String): TypeTypeApi {
        val normalized = if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TypeTypeApi::class.java)
    }
}
