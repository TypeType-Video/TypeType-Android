package dev.typetype.android.data.stream

import java.io.IOException
import java.util.concurrent.CancellationException
import dev.typetype.android.data.network.PlaybackNetworkObserver
import dev.typetype.android.data.network.PlaybackNetworkState
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.Response

class TransientPlaybackRequestTest {
    @Test
    fun `transient responses recover with bounded backoff`() = runBlocking {
        val responses = ArrayDeque(
            listOf(
                failure(503),
                failure(502),
                Response.success("ready"),
            ),
        )
        val pauses = mutableListOf<Long>()

        val response = transientPlaybackRequest(pauses::add) { responses.removeFirst() }

        assertEquals("ready", response.body())
        assertEquals(listOf(500L, 1_000L), pauses)
    }

    @Test
    fun `retry after controls transient response delay`() = runBlocking {
        val responses = ArrayDeque(
            listOf(
                failure(503, mapOf("Retry-After" to "2")),
                Response.success("ready"),
            ),
        )
        val pauses = mutableListOf<Long>()

        transientPlaybackRequest(pauses::add) { responses.removeFirst() }

        assertEquals(listOf(2_000L), pauses)
    }

    @Test
    fun `transport failure recovers without creating another request chain`() = runBlocking {
        var attempt = 0
        val pauses = mutableListOf<Long>()

        val response = transientPlaybackRequest(pauses::add) {
            attempt++
            if (attempt == 1) throw IOException("offline")
            Response.success("ready")
        }

        assertEquals("ready", response.body())
        assertEquals(2, attempt)
        assertEquals(listOf(500L), pauses)
    }

    @Test
    fun `offline transport waits for connectivity without backoff requests`() = runBlocking {
        val network = RestoringNetworkObserver()
        var attempt = 0
        val pauses = mutableListOf<Long>()

        val response = transientPlaybackRequest(pauses::add, network) {
            attempt++
            if (attempt == 1) throw IOException("offline")
            Response.success("ready")
        }

        assertEquals("ready", response.body())
        assertEquals(2, attempt)
        assertEquals(emptyList<Long>(), pauses)
        assertEquals(1, network.waitCount)
    }

    @Test
    fun `new route resets the connected transport retry budget`() = runBlocking {
        val network = MutableNetworkObserver(PlaybackNetworkState(true, 3L))
        var attempt = 0
        val pauses = mutableListOf<Long>()

        val response = transientPlaybackRequest(
            pause = { delayMs ->
                pauses += delayMs
                if (pauses.size == 8) {
                    network.state = PlaybackNetworkState(true, 4L)
                }
            },
            network = network,
        ) {
            attempt++
            if (attempt <= 9) throw IOException("handover")
            Response.success("ready")
        }

        assertEquals("ready", response.body())
        assertEquals(10, attempt)
        assertEquals(500L, pauses.last())
    }

    @Test
    fun `connected transport failure becomes terminal after extended budget`() {
        var attempt = 0
        val pauses = mutableListOf<Long>()

        assertThrows(IOException::class.java) {
            runBlocking {
                transientPlaybackRequest<String>(pauses::add) {
                    attempt++
                    throw IOException("unreachable")
                }
            }
        }

        assertEquals(9, attempt)
        assertEquals(8, pauses.size)
    }

    @Test
    fun `permanent response is returned without retry`() = runBlocking {
        var attempt = 0

        val response = transientPlaybackRequest({ error("unexpected pause") }) {
            attempt++
            failure<String>(400)
        }

        assertEquals(400, response.code())
        assertEquals(1, attempt)
    }

    @Test
    fun `transient response becomes terminal after retry budget`() = runBlocking {
        var attempt = 0
        val pauses = mutableListOf<Long>()

        val response = transientPlaybackRequest(pauses::add) {
            attempt++
            failure<String>(503)
        }

        assertEquals(503, response.code())
        assertEquals(25, attempt)
        assertEquals(24, pauses.size)
        assertEquals(listOf(500L, 1_000L, 2_000L, 3_000L), pauses.take(4))
        assertEquals(3_000L, pauses.last())
    }

    @Test
    fun `cancellation during backoff stops retry loop`() {
        val cancellation = CancellationException("cancelled")

        val thrown = assertThrows(CancellationException::class.java) {
            runBlocking {
                transientPlaybackRequest<String>({ throw cancellation }) {
                    failure(503)
                }
            }
        }

        assertSame(cancellation, thrown)
    }

    private fun <T> failure(
        code: Int,
        headers: Map<String, String> = emptyMap(),
    ): Response<T> {
        val raw = okhttp3.Response.Builder()
            .request(okhttp3.Request.Builder().url("https://example.invalid").build())
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(code)
            .message("failure")
            .apply { headers.forEach { (name, value) -> addHeader(name, value) } }
            .body("{}".toResponseBody())
            .build()
        return Response.error("{}".toResponseBody(), raw)
    }
}

private open class MutableNetworkObserver(
    var state: PlaybackNetworkState,
) : PlaybackNetworkObserver {
    override fun snapshot(): PlaybackNetworkState = state

    override suspend fun awaitAvailableAfter(generation: Long, timeoutMs: Long): Boolean = false
}

private class RestoringNetworkObserver :
    MutableNetworkObserver(PlaybackNetworkState(false, 0L)) {
    var waitCount = 0

    override suspend fun awaitAvailableAfter(generation: Long, timeoutMs: Long): Boolean {
        waitCount++
        state = PlaybackNetworkState(true, generation + 1L)
        return true
    }
}
