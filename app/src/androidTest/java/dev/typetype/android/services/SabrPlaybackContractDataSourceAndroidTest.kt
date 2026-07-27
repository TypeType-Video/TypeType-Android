package dev.typetype.android.services

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.typetype.android.domain.stream.SabrPlaybackBinding
import dev.typetype.android.domain.stream.SabrPlaybackSession
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@UnstableApi
@RunWith(AndroidJUnit4::class)
class SabrPlaybackContractDataSourceAndroidTest {
    @Test
    fun jsonMediaResponseBecomesRetryableHttp202() {
        val source = SabrPlaybackContractDataSource(
            ResponseDataSource(
                body = """{"status":"preparing"}""".toByteArray(),
                headers = mapOf("Content-Type" to listOf("application/json")),
            ),
            BINDING,
        )

        val failure = runCatching {
            source.open(DataSpec(Uri.parse(mediaUrl("137/segment/1"))))
        }.exceptionOrNull()

        assertTrue(failure is HttpDataSource.InvalidResponseCodeException)
        assertEquals(202, (failure as HttpDataSource.InvalidResponseCodeException).responseCode)
    }

    @Test
    fun jsonSegmentResponseUsesBoundedRetryDelay() {
        val body = """{"status":"preparing","retryAfterMs":500}""".toByteArray()
        val request = DataSpec(Uri.parse(mediaUrl("137/segment/2")))
        val source = SabrPlaybackContractDataSource(
            ResponseDataSource(body, mapOf("Content-Type" to listOf("application/json"))),
            BINDING,
        )

        val failure = runCatching { source.open(request) }.exceptionOrNull()
            as HttpDataSource.InvalidResponseCodeException

        assertArrayEquals(body, failure.responseBody)
        assertTrue(IOException("source", failure).isRecoverableSabrSessionFailure())
        val policy = SabrLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(5))
        assertEquals(500L, policy.getRetryDelayMsFor(failure.toLoadErrorInfo(request)))
        assertEquals(500L, policy.getRetryDelayMsFor(failure.toLoadErrorInfo(request, 59)))
        assertEquals(C.TIME_UNSET, policy.getRetryDelayMsFor(failure.toLoadErrorInfo(request, 60)))
        assertEquals(5, policy.getMinimumLoadableRetryCount(C.DATA_TYPE_MEDIA))
    }

    @Test
    fun transientGatewayFailureUsesTheSharedNetworkBudget() {
        val request = DataSpec(Uri.parse(mediaUrl("137/segment/2")))
        val failure = HttpDataSource.InvalidResponseCodeException(
            503,
            "Gateway unavailable",
            null,
            mapOf("Retry-After" to listOf("2")),
            request,
            byteArrayOf(),
        )
        val policy = SabrLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(5))

        assertEquals(2_000L, policy.getRetryDelayMsFor(failure.toLoadErrorInfo(request)))
        assertEquals(C.TIME_UNSET, policy.getRetryDelayMsFor(failure.toLoadErrorInfo(request, 25)))
    }

    @Test
    fun authenticationFailureIsNotHiddenByPlaybackRecovery() {
        val request = DataSpec(Uri.parse(mediaUrl("137/segment/2")))
        val failure = HttpDataSource.InvalidResponseCodeException(
            403,
            "Forbidden",
            null,
            emptyMap(),
            request,
            byteArrayOf(),
        )
        val policy = SabrLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(5))

        assertEquals(C.TIME_UNSET, policy.getRetryDelayMsFor(failure.toLoadErrorInfo(request)))
        assertFalse(failure.isRecoverableSabrSessionFailure())
    }

    @Test
    fun binaryInitializationRemainsReadable() {
        val body = byteArrayOf(0, 0, 0, 24, 102, 116, 121, 112)
        val source = SabrPlaybackContractDataSource(
            ResponseDataSource(body, mapOf("Content-Type" to listOf("video/mp4"))),
            BINDING,
        )
        val buffer = ByteArray(body.size)

        assertEquals(body.size.toLong(), source.open(DataSpec(Uri.parse(mediaUrl("137/init")))))
        assertEquals(body.size, source.read(buffer, 0, buffer.size))
        assertArrayEquals(body, buffer)
        source.close()
    }

    @Test
    fun transportValidationFollowsTheGenerationAcceptedByTheWindowDriver() {
        val state = SabrPlaybackTransportState(BINDING)
        val body = byteArrayOf(1, 2, 3)
        val source = SabrPlaybackContractDataSource(
            ResponseDataSource(body, mapOf("Content-Type" to listOf("video/mp4"))),
            state::currentBinding,
        )
        state.accept(
            SabrPlaybackSession(
                sessionId = "session",
                manifestUrl = "https://instance.example/api/sabr/playback/session/manifest",
                generation = 1L,
                videoItag = 137,
                audioItag = 140,
                audioTrackId = null,
            ),
        )
        val request = DataSpec(
            Uri.parse(
                "https://instance.example/api/sabr/playback/session/137/segment/2?generation=1",
            ),
        )

        assertEquals(body.size.toLong(), source.open(request))
        source.close()
    }

    @Test
    fun offOriginSegmentResponseIsRejected() {
        val request = DataSpec(Uri.parse(mediaUrl("137/segment/2")))
        val upstream = ResponseDataSource(
            byteArrayOf(1, 2, 3),
            mapOf("Content-Type" to listOf("video/mp4")),
            Uri.parse(mediaUrl("137/segment/2").replace("instance.example", "outside.example")),
        )

        val failure = runCatching {
            SabrPlaybackContractDataSource(upstream, BINDING).open(request)
        }.exceptionOrNull()

        assertTrue(failure is IOException)
    }

    @Test
    fun streamMetadataSubtitleFailureDoesNotReplaceTheVideoSession() {
        val request = DataSpec(
            Uri.parse("https://instance.example/api/streams/youtube/subtitles/en.vtt"),
        )
        val failure = HttpDataSource.InvalidResponseCodeException(
            404,
            "Subtitle unavailable",
            null,
            emptyMap(),
            request,
            byteArrayOf(),
        )

        assertFalse(failure.isRecoverableSabrSessionFailure())
    }

}

private fun mediaUrl(path: String) =
    "https://instance.example/api/sabr/playback/session/$path?session=session&generation=0"

private val BINDING = SabrPlaybackBinding("session", 0L, 137, 140)

private fun IOException.toLoadErrorInfo(
    request: DataSpec,
    errorCount: Int = 1,
) = LoadErrorHandlingPolicy.LoadErrorInfo(
    LoadEventInfo(1L, request, 0L),
    MediaLoadData(1),
    this,
    errorCount,
)

@UnstableApi
private class ResponseDataSource(
    private val body: ByteArray,
    private val headers: Map<String, List<String>>,
    private val responseUri: Uri? = null,
) : DataSource {
    private var position = 0
    private var openedUri: Uri? = null

    override fun addTransferListener(transferListener: TransferListener) = Unit

    override fun open(dataSpec: DataSpec): Long {
        openedUri = responseUri ?: dataSpec.uri
        position = 0
        return body.size.toLong()
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (position >= body.size) return -1
        val count = minOf(length, body.size - position)
        body.copyInto(buffer, offset, position, position + count)
        position += count
        return count
    }

    override fun getUri(): Uri? = openedUri
    override fun getResponseHeaders(): Map<String, List<String>> = headers
    override fun close() { openedUri = null }
}
