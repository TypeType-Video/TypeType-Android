package dev.typetype.android.data.feed

import dev.typetype.android.data.network.RetrofitFactory
import dev.typetype.android.data.network.TypeTypeFeedApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class SubscriptionFeedLiveContractTest {
    @Test
    fun configuredInstanceSupportsReadyAndContinuationPages() {
        runBlocking {
            val baseUrl = System.getenv(BASE_URL_ENV)
            val accessToken = System.getenv(ACCESS_TOKEN_ENV)
            assumeTrue(!baseUrl.isNullOrBlank() && !accessToken.isNullOrBlank())

            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("Authorization", "Bearer $accessToken")
                            .build(),
                    )
                }
                .build()
            val api = RetrofitFactory(
                sessionClient = client,
                json = Json { ignoreUnknownKeys = true },
            ).createWithClient(requireNotNull(baseUrl), TypeTypeFeedApi::class.java, client)
            val feedClient = SubscriptionFeedClient()

            val first = feedClient.load(api, null, PAGE_SIZE, null, verifyOwner = {})
            assertTrue(first.generation > 0L)
            assertTrue(first.generatedAtMillis > 0L)
            assertTrue(first.videos.isNotEmpty())
            assertTrue(first.videos.all { it.id.isNotBlank() && it.url.isNotBlank() })

            val continuation = feedClient.load(
                api = api,
                cursor = requireNotNull(first.nextCursor),
                limit = PAGE_SIZE,
                expectedGeneration = first.generation,
                verifyOwner = {},
            )
            assertEquals(first.generation, continuation.generation)
            assertTrue(continuation.videos.isNotEmpty())
            assertTrue(first.videos.map { it.url }.none(continuation.videos.map { it.url }::contains))
        }
    }

    private companion object {
        const val BASE_URL_ENV = "TYPETYPE_TEST_BASE_URL"
        const val ACCESS_TOKEN_ENV = "TYPETYPE_TEST_ACCESS_TOKEN"
        const val PAGE_SIZE = 1
    }
}
