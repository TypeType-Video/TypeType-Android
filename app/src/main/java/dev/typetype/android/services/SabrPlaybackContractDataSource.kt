package dev.typetype.android.services

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener
import dev.typetype.android.core.error.CodedFailure
import dev.typetype.android.domain.stream.SabrPlaybackBinding
import java.io.ByteArrayOutputStream
import java.io.IOException
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@UnstableApi
internal class SabrPlaybackContractDataSource(
    private val upstream: DataSource,
    private val expectedBinding: () -> SabrPlaybackBinding?,
) : DataSource {
    constructor(
        upstream: DataSource,
        expectedBinding: SabrPlaybackBinding?,
    ) : this(upstream, { expectedBinding })

    private var openedUpstream = false
    private var openedUri: Uri? = null
    private var headers: Map<String, List<String>> = emptyMap()

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        openedUpstream = true
        val length = upstream.open(dataSpec)
        openedUri = upstream.uri ?: dataSpec.uri
        headers = upstream.responseHeaders
        if (dataSpec.isSabrPlaybackMedia()) {
            try {
                val binding = expectedBinding()
                    ?: throw IOException("Missing SABR playback binding")
                SabrMediaTransportContract.validateResponseUrl(
                    dataSpec.uri.toString(),
                    requireNotNull(openedUri).toString(),
                    binding,
                )
            } catch (failure: Exception) {
                upstream.close()
                openedUpstream = false
                throw failure
            }
        }
        if (!dataSpec.isSabrPlaybackPayload() || !headers.hasJsonContentType()) return length

        val responseBody = try {
            readResponseBody()
        } finally {
            upstream.close()
            openedUpstream = false
        }
        throw HttpDataSource.InvalidResponseCodeException(
            SABR_RETRY_RESPONSE_CODE,
            SABR_RETRY_RESPONSE_MESSAGE,
            IOException(SABR_RETRY_RESPONSE_MESSAGE),
            headers,
            dataSpec,
            responseBody,
        )
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        upstream.read(buffer, offset, length)

    override fun getUri(): Uri? = openedUri ?: upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> =
        if (headers.isNotEmpty()) headers else upstream.responseHeaders

    override fun close() {
        if (openedUpstream) upstream.close()
        openedUpstream = false
        openedUri = null
        headers = emptyMap()
    }

    private fun readResponseBody(): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(2 * 1024)
        while (output.size() < MAX_ERROR_BODY_BYTES) {
            val limit = minOf(buffer.size, MAX_ERROR_BODY_BYTES - output.size())
            val read = upstream.read(buffer, 0, limit)
            if (read == -1) break
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    class Factory(
        private val upstream: DataSource.Factory,
        private val expectedBinding: () -> SabrPlaybackBinding?,
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource =
            SabrPlaybackContractDataSource(upstream.createDataSource(), expectedBinding)
    }
}

private object SabrMediaTransportContract {
    fun validateResponseUrl(
        requestUrl: String,
        responseUrl: String,
        binding: SabrPlaybackBinding,
    ) {
        val request = requestUrl.toHttpUrlOrNull() ?: invalidSabrMediaTransport()
        val response = responseUrl.toHttpUrlOrNull() ?: invalidSabrMediaTransport()
        if (request != response) invalidSabrMediaTransport()
        request.requireBinding(binding)
    }

    private fun HttpUrl.requireBinding(binding: SabrPlaybackBinding) {
        val segments = pathSegments
        val playbackIndex = segments.indexOfLast { it == "playback" }
        val sessionId = segments.getOrNull(playbackIndex + 1)
        val itag = segments.getOrNull(playbackIndex + 2)?.toIntOrNull()
        if (
            playbackIndex < 1 || segments.getOrNull(playbackIndex - 1) != "sabr" ||
            sessionId != binding.sessionId || itag !in setOf(binding.videoItag, binding.audioItag)
        ) {
            invalidSabrMediaTransport()
        }
        if (queryParameterNames.any { it !in ALLOWED_SABR_MEDIA_QUERY_PARAMETERS }) {
            invalidSabrMediaTransport()
        }
        if (queryParameterValues("generation") != listOf(binding.generation.toString())) {
            invalidSabrMediaTransport()
        }
        val sessionValues = queryParameterValues("session")
        if (sessionValues.isNotEmpty() && sessionValues != listOf(binding.sessionId)) {
            invalidSabrMediaTransport()
        }
    }
}

@UnstableApi
private fun DataSpec.isSabrPlaybackPayload(): Boolean =
    uri.pathSegments.isSabrPlaybackPayloadPath()

@UnstableApi
private fun DataSpec.isSabrPlaybackMedia(): Boolean =
    uri.pathSegments.isSabrPlaybackPayloadPath() && uri.pathSegments.lastOrNull() != "manifest"

internal fun List<String>.isSabrPlaybackPayloadPath(): Boolean {
    val manifest = takeLast(4)
    if (
        manifest.size == 4 && manifest[0] == "sabr" && manifest[1] == "playback" &&
        manifest[2].isNotBlank() && manifest[3] == "manifest"
    ) {
        return true
    }
    val initialization = takeLast(5)
    if (
        initialization.size == 5 && initialization[0] == "sabr" &&
        initialization[1] == "playback" && initialization[2].isNotBlank() &&
        initialization[3].toIntOrNull()?.let { it > 0 } == true && initialization[4] == "init"
    ) {
        return true
    }
    val segment = takeLast(6)
    return segment.size == 6 && segment[0] == "sabr" && segment[1] == "playback" &&
        segment[2].isNotBlank() && segment[3].toIntOrNull()?.let { it > 0 } == true &&
        segment[4] == "segment" && segment[5].toLongOrNull()?.let { it >= 0L } == true
}

internal fun Map<String, List<String>>.hasJsonContentType(): Boolean =
    entries.asSequence()
        .filter { it.key.equals("Content-Type", ignoreCase = true) }
        .flatMap { it.value.asSequence() }
        .map { it.substringBefore(';').trim() }
        .any { it.equals("application/json", ignoreCase = true) || it.endsWith("+json", ignoreCase = true) }

private const val SABR_RETRY_RESPONSE_CODE = 202
private const val SABR_RETRY_RESPONSE_MESSAGE = "SABR media is not ready"
private const val MAX_ERROR_BODY_BYTES = 8 * 1024
private const val SABR_CONTRACT_FAILURE_CODE = "youtube_sabr_contract_mismatch"
private val ALLOWED_SABR_MEDIA_QUERY_PARAMETERS = setOf("session", "generation")

private fun invalidSabrMediaTransport(): Nothing = throw SabrMediaTransportFailure()

private class SabrMediaTransportFailure :
    IOException("SABR media left its validated playback session"),
    CodedFailure {
    override val failureCode: String = SABR_CONTRACT_FAILURE_CODE
    override val requestId: String? = null
    override val statusCode: Int? = null
}
