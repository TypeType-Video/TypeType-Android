package video.typetype.tv.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import video.typetype.sdk.core.DownloadArtifact
import video.typetype.sdk.core.DownloadJob
import video.typetype.sdk.core.DownloaderApi
import video.typetype.sdk.core.TypeTypeByteSink
import video.typetype.sdk.core.TypeTypeResult

public data class TvSavedArtifact(
    val fileName: String,
    val location: String,
)

public class TvArtifactStore(
    context: Context,
) {
    private val appContext = context.applicationContext

    public suspend fun save(api: DownloaderApi, job: DownloadJob): Result<TvSavedArtifact> = withContext(Dispatchers.IO) {
        runCatching {
            val temporaryDirectory = File(appContext.noBackupFilesDir, "download-artifacts").apply { mkdirs() }
            val temporary = File(temporaryDirectory, "${safeJobId(job.id)}.part")
            try {
                val artifact = downloadArtifact(api, job.id, temporary)
                publish(temporary, job, artifact)
            } finally {
                temporary.delete()
            }
        }
    }

    private suspend fun downloadArtifact(api: DownloaderApi, jobId: String, target: File): DownloadArtifact {
        val existingBytes = target.length().takeIf { it > 0L }
        val artifact = appendArtifact(api, jobId, target, existingBytes)
        if (existingBytes != null && artifact.status != 206) {
            target.delete()
            return appendArtifact(api, jobId, target, null)
        }
        return artifact
    }

    private suspend fun appendArtifact(
        api: DownloaderApi,
        jobId: String,
        target: File,
        rangeStart: Long?,
    ): DownloadArtifact = FileOutputStream(target, rangeStart != null).use { output ->
        when (val result = api.artifact(
            jobId,
            TypeTypeByteSink { bytes, offset, length -> output.write(bytes, offset, length) },
            rangeStart,
        )) {
            is TypeTypeResult.Success -> result.value
            is TypeTypeResult.Failure -> error(result.error.toUserMessage())
        }
    }

    private fun publish(source: File, job: DownloadJob, artifact: DownloadArtifact): TvSavedArtifact {
        val fileName = safeFileName(
            artifact.fileName ?: job.resolved?.fileName ?: defaultFileName(job),
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            publishToMediaStore(source, fileName, artifact.contentType)
        } else {
            publishToAppDownloads(source, fileName)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun publishToMediaStore(source: File, fileName: String, contentType: String?): TvSavedArtifact {
        val resolver = appContext.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, contentType ?: "application/octet-stream")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/TypeType")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)) {
            "Android could not create the download destination"
        }
        try {
            requireNotNull(resolver.openOutputStream(uri, "w")) {
                "Android could not open the download destination"
            }.use { output -> source.inputStream().use { input -> input.copyTo(output) } }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
            return TvSavedArtifact(fileName, "Downloads/TypeType")
        } catch (exception: Exception) {
            resolver.delete(uri, null, null)
            throw exception
        }
    }

    private fun publishToAppDownloads(source: File, fileName: String): TvSavedArtifact {
        val root = requireNotNull(appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)) {
            "Android external storage is unavailable"
        }
        val directory = File(root, "TypeType").apply { mkdirs() }
        val destination = uniqueFile(directory, fileName)
        source.inputStream().use { input -> destination.outputStream().use { output -> input.copyTo(output) } }
        return TvSavedArtifact(destination.name, "TypeType downloads")
    }
}

private fun safeJobId(value: String): String = value.replace(Regex("[^A-Za-z0-9._-]"), "_").take(120)

private fun defaultFileName(job: DownloadJob): String {
    val title = job.title?.takeIf(String::isNotBlank) ?: "TypeType download"
    val extension = job.resolved?.container?.takeIf(String::isNotBlank) ?: "bin"
    return "$title.$extension"
}

private fun safeFileName(value: String): String = value.trim()
    .replace(Regex("[\\/:*?\"<>|\\p{Cntrl}]"), "_")
    .trim('.', ' ')
    .take(180)
    .ifBlank { "TypeType download.bin" }

private fun uniqueFile(directory: File, fileName: String): File {
    val initial = File(directory, fileName)
    if (!initial.exists()) return initial
    val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
    val base = if (extension.isEmpty()) fileName else fileName.removeSuffix(".$extension")
    var suffix = 2
    while (true) {
        val candidateName = if (extension.isEmpty()) "$base ($suffix)" else "$base ($suffix).$extension"
        val candidate = File(directory, candidateName)
        if (!candidate.exists()) return candidate
        suffix += 1
    }
}
