package dev.typetype.android.data.network

import android.os.Build
import dev.typetype.android.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

@Singleton
class UserAgentInterceptor @Inject constructor() : Interceptor {

    private val userAgent: String by lazy {
        "TypeType-Android/${BuildConfig.VERSION_NAME} " +
            "(Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL}; mobile)"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", userAgent)
            .build()
        return chain.proceed(request)
    }
}
