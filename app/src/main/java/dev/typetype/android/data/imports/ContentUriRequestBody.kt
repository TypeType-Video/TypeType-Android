package dev.typetype.android.data.imports

import android.content.ContentResolver
import android.net.Uri
import java.io.FileNotFoundException
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink

internal class ContentUriRequestBody(
    private val contentResolver: ContentResolver,
    private val uri: Uri,
    private val mediaType: MediaType,
    private val knownSize: Long?,
    private val maxBytes: Long,
) : RequestBody() {
    override fun contentType(): MediaType = mediaType

    override fun contentLength(): Long = knownSize ?: -1L

    override fun writeTo(sink: BufferedSink) {
        val input = contentResolver.openInputStream(uri)
            ?: throw FileNotFoundException("IMPORT_FILE_UNAVAILABLE")
        input.use {
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var written = 0L
            while (true) {
                val count = it.read(buffer)
                if (count < 0) return
                written += count
                if (written > maxBytes) error("IMPORT_FILE_TOO_LARGE")
                sink.write(buffer, 0, count)
            }
        }
    }
}
