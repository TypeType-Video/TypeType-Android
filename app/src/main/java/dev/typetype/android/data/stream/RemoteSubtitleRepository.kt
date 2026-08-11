package dev.typetype.android.data.stream

import dev.typetype.android.data.account.AccountScopeProvider
import dev.typetype.android.domain.stream.StreamSubtitleSource
import dev.typetype.android.domain.stream.SubtitleRepository
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody

@Singleton
class RemoteSubtitleRepository @Inject constructor(
    @Named("refresh") baseClient: OkHttpClient,
    private val activeAccountScope: AccountScopeProvider,
) : SubtitleRepository {
    private val client = baseClient.newBuilder()
        .cookieJar(CookieJar.NO_COOKIES)
        .retryOnConnectionFailure(false)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .removeHeader("Authorization")
                .removeHeader("Cookie")
                .removeHeader("Proxy-Authorization")
                .build()
            chain.proceed(request)
        }
        .build()

    override suspend fun load(source: StreamSubtitleSource): Result<ByteArray> = try {
        val scope = activeAccountScope.require()
        val bytes = withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(source.url)
                .header("Accept", "text/vtt, application/ttml+xml, text/xml;q=0.9")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) {
                    "Subtitle request failed with HTTP ${response.code}"
                }
                val body = requireNotNull(response.body) { "Empty subtitle response" }
                require(body.contentLength() <= MAX_SUBTITLE_BYTES) {
                    "Subtitle track is too large"
                }
                body.readBoundedBytes()
            }
        }
        activeAccountScope.verify(scope)
        Result.success(bytes)
    } catch (failure: CancellationException) {
        throw failure
    } catch (failure: Throwable) {
        Result.failure(failure)
    }

    private companion object {
        const val MAX_SUBTITLE_BYTES = 5 * 1024 * 1024
        const val READ_BUFFER_BYTES = 8 * 1024
    }

    private fun ResponseBody.readBoundedBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        byteStream().use { input ->
            val buffer = ByteArray(READ_BUFFER_BYTES)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= MAX_SUBTITLE_BYTES) {
                    "Subtitle track is too large"
                }
                output.write(buffer, 0, count)
            }
        }
        return output.toByteArray()
    }
}
