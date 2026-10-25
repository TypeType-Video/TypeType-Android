package dev.typetype.android.data.appupdate

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.domain.appupdate.AppUpdateFailure
import dev.typetype.android.domain.appupdate.AppUpdateFailureReason
import dev.typetype.android.domain.appupdate.AppUpdateFileDownloader
import dev.typetype.android.domain.appupdate.AppUpdateRelease
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class AppUpdateDownloader @Inject constructor(
    @Named("appUpdates") private val client: OkHttpClient,
    @ApplicationContext private val context: Context,
) : AppUpdateFileDownloader {
    override suspend fun download(
        release: AppUpdateRelease,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, UPDATE_DIRECTORY).apply { mkdirs() }
        val safeTag = release.tagName.replace(UNSAFE_FILE_CHARACTERS, "_")
        val part = File(directory, "$safeTag.apk.part")
        val target = File(directory, "$safeTag.apk")
        if (target.exists() && target.length() == release.apkSizeBytes && hasExpectedDigest(target, release.sha256)) {
            return@withContext target
        }
        target.delete()
        if (part.length() > release.apkSizeBytes) part.delete()
        var lastFailure: IOException? = null

        repeat(MAX_ATTEMPTS) { attempt ->
            val currentSize = part.length()
            val request = Request.Builder()
                .url(release.apkUrl)
                .header("Accept", "application/octet-stream")
                .apply {
                    if (currentSize > 0) header("Range", "bytes=$currentSize-")
                }
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    if (response.code == 416) {
                        if (currentSize == release.apkSizeBytes && hasExpectedDigest(part, release.sha256)) {
                            if (!part.renameTo(target)) throw IOException("Could not stage the APK")
                            return@withContext target
                        }
                        if (currentSize == release.apkSizeBytes) {
                            part.delete()
                            throw AppUpdateFailure(AppUpdateFailureReason.INVALID_CHECKSUM)
                        }
                        part.delete()
                        lastFailure = IOException("Update server rejected the partial download")
                        return@use
                    }
                    val append = response.code == 206 && currentSize > 0
                    if (response.code != 200 && !append) {
                        throw IOException("Update download returned HTTP " + response.code)
                    }
                    if (append) {
                        val contentRange = contentRange(response.header("Content-Range"))
                        if (
                            contentRange?.start != currentSize ||
                            contentRange.total != release.apkSizeBytes ||
                            contentRange.endInclusive >= contentRange.total
                        ) {
                            throw IOException("Update download returned an invalid range")
                        }
                    }
                    if (!append && currentSize > 0) part.delete()
                    val body = response.body
                    val responseLength = body.contentLength()
                    val totalSize = release.apkSizeBytes
                    if (responseLength > totalSize || totalSize > MAX_APK_SIZE_BYTES) {
                        throw AppUpdateFailure(AppUpdateFailureReason.INVALID_RELEASE)
                    }
                    val initialSize = if (append) currentSize else 0L
                    body.byteStream().use { input ->
                        FileOutputStream(part, append).buffered().use { output ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var written = 0L
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                if (initialSize + written + count > totalSize) {
                                    throw AppUpdateFailure(AppUpdateFailureReason.INVALID_RELEASE)
                                }
                                output.write(buffer, 0, count)
                                written += count
                                onProgress(initialSize + written, totalSize)
                            }
                        }
                    }
                }
                if (part.length() == release.apkSizeBytes) {
                    if (!hasExpectedDigest(part, release.sha256)) {
                        part.delete()
                        throw AppUpdateFailure(AppUpdateFailureReason.INVALID_CHECKSUM)
                    }
                    if (!part.renameTo(target)) throw IOException("Could not stage the APK")
                    return@withContext target
                }
                lastFailure = IOException("Update download ended before the APK was complete")
            } catch (failure: AppUpdateFailure) {
                throw failure
            } catch (failure: IOException) {
                lastFailure = failure
            }
            if (attempt + 1 < MAX_ATTEMPTS) Thread.sleep(RETRY_DELAY_MILLIS * (attempt + 1))
        }
        throw AppUpdateFailure(AppUpdateFailureReason.NETWORK, lastFailure)
    }

    private fun hasExpectedDigest(file: File, expected: String): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }.equals(expected, ignoreCase = true)
    }

    private fun contentRange(value: String?): ContentRange? {
        val values = CONTENT_RANGE.matchEntire(value.orEmpty())?.groupValues ?: return null
        return ContentRange(
            start = values[1].toLongOrNull() ?: return null,
            endInclusive = values[2].toLongOrNull() ?: return null,
            total = values[3].toLongOrNull() ?: return null,
        )
    }

    private data class ContentRange(
        val start: Long,
        val endInclusive: Long,
        val total: Long,
    )

    private companion object {
        val UNSAFE_FILE_CHARACTERS = Regex("[^A-Za-z0-9._-]")
        val CONTENT_RANGE = Regex("""bytes (\d+)-(\d+)/(\d+)""")
        const val UPDATE_DIRECTORY = "app-updates"
        const val MAX_ATTEMPTS = 4
        const val RETRY_DELAY_MILLIS = 350L
        const val BUFFER_SIZE = 32 * 1024
        const val MAX_APK_SIZE_BYTES = 200L * 1024 * 1024
    }
}
