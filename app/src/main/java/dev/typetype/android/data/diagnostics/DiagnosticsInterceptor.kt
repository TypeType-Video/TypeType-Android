package dev.typetype.android.data.diagnostics

import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import dev.typetype.android.data.network.NetworkRequestScope
import okhttp3.Interceptor
import okhttp3.Response

@Singleton
class DiagnosticsInterceptor @Inject constructor(
    private val repository: LocalDiagnosticsRepository,
    private val sabrSanitizer: SabrDiagnosticSanitizer,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val scope = repository.scopeFor(request.url, request.tag(NetworkRequestScope::class.java))
        val startedAt = System.nanoTime()
        return try {
            val response = chain.proceed(request)
            if (scope != null) {
                repository.record(
                    scope = scope,
                    method = request.method,
                    statusCode = response.code,
                    durationMillis = elapsedMillis(startedAt),
                    requestId = response.header("X-Request-Id"),
                    sabr = sabrSanitizer.sanitize(scope.route, request, response),
                )
            }
            response
        } catch (failure: IOException) {
            if (scope != null) {
                repository.record(
                    scope = scope,
                    method = request.method,
                    statusCode = null,
                    durationMillis = elapsedMillis(startedAt),
                    requestId = null,
                    sabr = sabrSanitizer.sanitizeFailure(scope.route, request),
                )
            }
            throw failure
        }
    }

    private fun elapsedMillis(startedAt: Long): Long =
        (System.nanoTime() - startedAt).coerceAtLeast(0) / 1_000_000L
}
