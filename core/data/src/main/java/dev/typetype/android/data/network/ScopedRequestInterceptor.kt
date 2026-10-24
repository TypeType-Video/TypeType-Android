package dev.typetype.android.data.network

import okhttp3.Interceptor
import okhttp3.Response

class ScopedRequestInterceptor(
    private val scope: NetworkRequestScope,
    private val bearerToken: () -> String?,
) : Interceptor {
    private val endpoint = CurrentServerEndpoint(scope.serverId, scope.baseUrl)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!endpoint.owns(request.url)) return chain.proceed(request)
        val builder = request.newBuilder().tag(NetworkRequestScope::class.java, scope)
        bearerToken()?.let { builder.header("Authorization", "Bearer $it") }
        return chain.proceed(builder.build())
    }
}
