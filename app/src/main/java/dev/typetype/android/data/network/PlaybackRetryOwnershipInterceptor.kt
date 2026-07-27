package dev.typetype.android.data.network

import okhttp3.Interceptor
import okhttp3.Response

internal object PlaybackRetryOwnershipInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        return if (
            response.code == HTTP_SERVICE_UNAVAILABLE &&
            response.header(RETRY_AFTER_HEADER)?.trim() == IMMEDIATE_RETRY_SECONDS
        ) {
            response.newBuilder()
                .removeHeader(RETRY_AFTER_HEADER)
                .build()
        } else {
            response
        }
    }
}

private const val HTTP_SERVICE_UNAVAILABLE = 503
private const val RETRY_AFTER_HEADER = "Retry-After"
private const val IMMEDIATE_RETRY_SECONDS = "0"
