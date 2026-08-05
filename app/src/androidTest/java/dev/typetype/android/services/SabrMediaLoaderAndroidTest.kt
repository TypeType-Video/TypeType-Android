package dev.typetype.android.services

import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.Loader
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.typetype.android.data.network.PlaybackRetryOwnershipInterceptor
import dev.typetype.android.domain.stream.SabrPlaybackBinding
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@UnstableApi
@RunWith(AndroidJUnit4::class)
class SabrMediaLoaderAndroidTest {
    private lateinit var server: MockWebServer

    @Before
    fun startServer() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun stopServer() {
        server.shutdown()
    }

    @Test
    fun healthyMediaUsesOneRequestWithoutRetry() {
        server.enqueue(mediaResponse())

        val result = loadMedia()

        assertTrue(result.completed)
        assertNull(result.failure)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun preparingMediaRetriesTheSameRequestThenCompletes() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"status":"preparing","retryAfterMs":0}"""),
        )
        server.enqueue(mediaResponse())

        val result = loadMedia()

        assertTrue(result.completed)
        assertNull(result.failure)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun transientGatewayFailureRetriesThenCompletes() {
        server.enqueue(
            MockResponse()
                .setResponseCode(503)
                .setHeader("Retry-After", "0"),
        )
        server.enqueue(mediaResponse())

        val result = loadMedia()

        assertTrue(result.completed)
        assertNull(result.failure)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun repeatedConnectionCutsRecoverWithoutChangingTheMediaRequest() {
        repeat(CONNECTION_CUT_COUNT) {
            server.enqueue(
                MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST),
            )
        }
        server.enqueue(mediaResponse())

        val result = loadMedia(
            timeoutSeconds = 20,
            retryDelayTransform = { 0L },
        )

        assertTrue(result.completed)
        assertNull(result.failure)
        assertEquals(CONNECTION_CUT_COUNT + 1, server.requestCount)
    }

    @Test
    fun permanentFailureStopsWithoutRetry() {
        server.enqueue(MockResponse().setResponseCode(403))

        val result = loadMedia()

        assertEquals(403, result.failure?.responseCode)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun transientGatewayFailureStopsAtSharedBudget() {
        repeat(25) {
            server.enqueue(
                MockResponse()
                    .setResponseCode(503)
                    .setHeader("Retry-After", "0"),
            )
        }

        val result = loadMedia(
            timeoutSeconds = 20,
            retryDelayTransform = { 0L },
        )

        assertEquals(503, result.failure?.responseCode)
        assertEquals(25, server.requestCount)
    }

    private fun loadMedia(
        timeoutSeconds: Long = 10,
        retryDelayTransform: (Long) -> Long = { it },
    ): LoadResult {
        val dataSpec = DataSpec(
            Uri.parse(
                server.url(
                    "/api/sabr/playback/session/137/segment/2" +
                        "?session=session&generation=0",
                ).toString(),
            ),
        )
        val sourceFactory = SabrPlaybackContractDataSource.Factory(
            OkHttpDataSource.Factory(
                OkHttpClient.Builder()
                    .retryOnConnectionFailure(false)
                    .addNetworkInterceptor(PlaybackRetryOwnershipInterceptor)
                    .build(),
            ),
            { BINDING },
        )
        val policy = SabrLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(5))
        val completed = AtomicBoolean(false)
        val failure = AtomicReference<HttpDataSource.InvalidResponseCodeException?>()
        val errorCount = AtomicInteger()
        val finished = CountDownLatch(1)
        val loader = Loader("SabrMediaLoaderAndroidTest")
        val loadable = DataSourceLoadable(sourceFactory, dataSpec)
        val callback = MediaLoaderCallback(
            dataSpec = dataSpec,
            policy = policy,
            completed = completed,
            failure = failure,
            finished = finished,
            observedErrorCount = errorCount,
            retryDelayTransform = retryDelayTransform,
        )

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            loader.startLoading(
                loadable,
                callback,
                policy.getMinimumLoadableRetryCount(C.DATA_TYPE_MEDIA),
            )
        }
        val didFinish = finished.await(timeoutSeconds, TimeUnit.SECONDS)
        assertTrue(
            "Media3 loader did not finish after ${server.requestCount} requests " +
                "and ${errorCount.get()} loader errors",
            didFinish,
        )
        InstrumentationRegistry.getInstrumentation().runOnMainSync(loader::release)
        return LoadResult(completed.get(), failure.get())
    }

    private fun mediaResponse(): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "video/mp4")
            .setBody("media")

    private data class LoadResult(
        val completed: Boolean,
        val failure: HttpDataSource.InvalidResponseCodeException?,
    )

    private class MediaLoaderCallback(
        private val dataSpec: DataSpec,
        private val policy: LoadErrorHandlingPolicy,
        private val completed: AtomicBoolean,
        private val failure: AtomicReference<HttpDataSource.InvalidResponseCodeException?>,
        private val finished: CountDownLatch,
        private val observedErrorCount: AtomicInteger,
        private val retryDelayTransform: (Long) -> Long,
    ) : Loader.Callback<DataSourceLoadable> {
        override fun onLoadCompleted(
            loadable: DataSourceLoadable,
            elapsedRealtimeMs: Long,
            loadDurationMs: Long,
        ) {
            completed.set(true)
            finished.countDown()
        }

        override fun onLoadCanceled(
            loadable: DataSourceLoadable,
            elapsedRealtimeMs: Long,
            loadDurationMs: Long,
            released: Boolean,
        ) {
            finished.countDown()
        }

        override fun onLoadError(
            loadable: DataSourceLoadable,
            elapsedRealtimeMs: Long,
            loadDurationMs: Long,
            error: IOException,
            errorCount: Int,
        ): Loader.LoadErrorAction {
            observedErrorCount.set(errorCount)
            val delayMs = policy.getRetryDelayMsFor(
                LoadErrorHandlingPolicy.LoadErrorInfo(
                    LoadEventInfo(1L, dataSpec, elapsedRealtimeMs),
                    MediaLoadData(C.DATA_TYPE_MEDIA),
                    error,
                    errorCount,
                ),
            )
            if (delayMs != C.TIME_UNSET) {
                return Loader.createRetryAction(false, retryDelayTransform(delayMs))
            }
            failure.set(error.findHttpResponse())
            Handler(Looper.getMainLooper()).post(finished::countDown)
            return Loader.DONT_RETRY_FATAL
        }
    }

    private class DataSourceLoadable(
        private val sourceFactory: DataSource.Factory,
        private val dataSpec: DataSpec,
    ) : Loader.Loadable {
        override fun cancelLoad() = Unit

        override fun load() {
            val source = sourceFactory.createDataSource()
            try {
                source.open(dataSpec)
                val buffer = ByteArray(1_024)
                while (source.read(buffer, 0, buffer.size) != C.RESULT_END_OF_INPUT) {}
            } finally {
                source.close()
            }
        }
    }
}

private fun IOException.findHttpResponse(): HttpDataSource.InvalidResponseCodeException? {
    var current: Throwable? = this
    repeat(8) {
        val response = current as? HttpDataSource.InvalidResponseCodeException
        if (response != null) return response
        current = current?.cause?.takeUnless { it === current }
    }
    return null
}

private val BINDING = SabrPlaybackBinding("session", 0L, 137, 140)
private const val CONNECTION_CUT_COUNT = 8
