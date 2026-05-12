package dev.typetype.android.data.download

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.data.network.ApiBaseUrlHolder
import dev.typetype.android.data.network.DownloaderGatewayApiHolder
import dev.typetype.android.data.network.dto.CreateDownloadJobRequest
import dev.typetype.android.data.network.dto.DownloadJobOptionsDto
import dev.typetype.android.data.network.dto.DownloadJobResponse
import dev.typetype.android.data.network.dto.DownloadJobStatusDto
import dev.typetype.android.data.network.extractServerErrorMessage
import dev.typetype.android.domain.download.DownloadItem
import dev.typetype.android.domain.download.DownloadProgress
import dev.typetype.android.domain.download.DownloadRepository
import dev.typetype.android.domain.download.DownloadStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Singleton
class AndroidDownloadRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val apiHolder: DownloaderGatewayApiHolder,
    private val baseUrlHolder: ApiBaseUrlHolder,
    private val json: Json,
) : DownloadRepository {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val downloads = MutableStateFlow(loadStoredEntries().toDownloadItems())

    override fun observeDownloads(): Flow<List<DownloadItem>> {
        refreshDownloadItems()
        return downloads.asStateFlow()
    }

    override fun refreshDownloads() {
        refreshDownloadItems()
    }

    override fun downloadVideo(
        videoUrl: String,
        title: String,
        quality: String,
    ): Flow<DownloadProgress> = flow {
        val api = apiHolder.require()
        val createdResponse = api.createJob(
            CreateDownloadJobRequest(
                url = videoUrl,
                options = DownloadJobOptionsDto(quality = normalizedQuality(quality)),
            ),
        )
        if (!createdResponse.isSuccessful) error(extractServerErrorMessage(createdResponse))
        val created = createdResponse.body() ?: error("Empty downloader response")
        emit(DownloadProgress.Queued(cached = created.cached))

        repeat(MAX_JOB_POLLS) {
            val jobResponse = api.job(created.id)
            if (!jobResponse.isSuccessful) error(extractServerErrorMessage(jobResponse))
            val job = jobResponse.body() ?: error("Empty downloader job")
            when (job.status) {
                DownloadJobStatusDto.Queued -> emit(DownloadProgress.Queued(cached = created.cached))
                DownloadJobStatusDto.Running -> emit(
                    DownloadProgress.Running(
                        progressPercent = job.progressPercent,
                        stage = job.stage,
                    ),
                )
                DownloadJobStatusDto.Done -> {
                    val fileName = job.outputFileName(title)
                    val downloadId = enqueueArtifactDownload(job.id, fileName, title)
                    saveStoredEntry(
                        StoredDownloadEntry(
                            downloadId = downloadId,
                            title = title.ifBlank { job.title }.ifBlank { DEFAULT_VIDEO_NAME },
                            fileName = fileName,
                            createdAtMillis = System.currentTimeMillis(),
                        ),
                    )
                    emit(DownloadProgress.Enqueued(downloadId, fileName))
                    return@flow
                }
                DownloadJobStatusDto.Failed -> error(job.error?.takeIf { it.isNotBlank() } ?: "Download failed")
            }
            delay(JOB_POLL_DELAY_MS)
        }
        error("Download timed out")
    }.flowOn(Dispatchers.IO)

    override suspend fun openDownload(downloadId: Long): Result<Unit> = runCatching {
        val uri = downloadManager().getUriForDownloadedFile(downloadId)
            ?: error("Download is not ready")
        val intent = Intent(Intent.ACTION_VIEW)
            .setData(uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun enqueueArtifactDownload(jobId: String, fileName: String, title: String): Long {
        val baseUrl = baseUrlHolder.currentBaseUrl?.trimEnd('/') ?: error("No server is currently selected")
        val uri = Uri.parse("$baseUrl/downloader/jobs/$jobId/artifact?download=1")
        val request = DownloadManager.Request(uri)
            .setTitle(title.ifBlank { fileName })
            .setDescription(fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        return downloadManager().enqueue(request)
    }

    private fun downloadManager(): DownloadManager =
        context.getSystemService(DownloadManager::class.java) ?: error("Download manager unavailable")

    private fun DownloadJobResponse.outputFileName(fallbackTitle: String): String {
        val extension = resolved?.container?.takeIf { it.isNotBlank() }
            ?: resolved?.fileName?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() }
            ?: DEFAULT_VIDEO_EXTENSION
        val baseName = fallbackTitle.ifBlank { title }.ifBlank { DEFAULT_VIDEO_NAME }
        return "${sanitizeFileName(baseName)}.$extension"
    }

    private fun saveStoredEntry(entry: StoredDownloadEntry) {
        val updated = (loadStoredEntries().filterNot { it.downloadId == entry.downloadId } + entry)
            .sortedByDescending { it.createdAtMillis }
            .take(MAX_STORED_DOWNLOADS)
        prefs.edit()
            .putString(KEY_DOWNLOADS, json.encodeToString(ListSerializer(StoredDownloadEntry.serializer()), updated))
            .apply()
        downloads.value = updated.toDownloadItems()
    }

    private fun loadStoredEntries(): List<StoredDownloadEntry> {
        val raw = prefs.getString(KEY_DOWNLOADS, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(StoredDownloadEntry.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    private fun refreshDownloadItems() {
        val storedItems = loadStoredEntries().toDownloadItems()
        val storedIds = storedItems.map { it.downloadId }.toSet()
        val managerItems = queryDownloadManagerItems().filterNot { it.downloadId in storedIds }
        downloads.value = (storedItems + managerItems).sortedByDescending { it.createdAtMillis }
    }

    private fun List<StoredDownloadEntry>.toDownloadItems(): List<DownloadItem> =
        map { entry ->
            val status = queryDownloadStatus(entry.downloadId)
            DownloadItem(
                downloadId = entry.downloadId,
                title = entry.title,
                fileName = entry.fileName,
                status = status.first,
                progressPercent = status.second,
                createdAtMillis = entry.createdAtMillis,
            )
        }.sortedByDescending { it.createdAtMillis }

    private fun queryDownloadStatus(downloadId: Long): Pair<DownloadStatus, Int?> {
        val query = DownloadManager.Query().setFilterById(downloadId)
        downloadManager().query(query)?.use { cursor ->
            if (!cursor.moveToFirst()) return DownloadStatus.Failed to null
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val bytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            return DownloadManagerCursorReader.status(cursor, statusIndex, bytesIndex, totalIndex)
        }
        return DownloadStatus.Failed to null
    }

    private fun queryDownloadManagerItems(): List<DownloadItem> {
        downloadManager().query(DownloadManager.Query())?.use { cursor ->
            val items = mutableListOf<DownloadItem>()
            val idIndex = cursor.getColumnIndex(DownloadManager.COLUMN_ID)
            val titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
            val fileNameIndex = cursor.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION)
            val modifiedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP)
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val bytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            while (cursor.moveToNext()) {
                val id = DownloadManagerCursorReader.longOrNull(cursor, idIndex) ?: continue
                val status = DownloadManagerCursorReader.status(cursor, statusIndex, bytesIndex, totalIndex)
                val title = DownloadManagerCursorReader.stringOrNull(cursor, titleIndex)
                    ?.takeIf { it.isNotBlank() } ?: DEFAULT_VIDEO_NAME
                val fileName = DownloadManagerCursorReader.stringOrNull(cursor, fileNameIndex)
                    ?.takeIf { it.isNotBlank() } ?: title
                val modified = DownloadManagerCursorReader.longOrNull(cursor, modifiedIndex)
                    ?.takeIf { it > 0L } ?: 0L
                items += DownloadItem(
                    downloadId = id,
                    title = title,
                    fileName = fileName,
                    status = status.first,
                    progressPercent = status.second,
                    createdAtMillis = modified,
                )
            }
            return items
        }
        return emptyList()
    }

    private fun normalizedQuality(value: String): String {
        val clean = value.trim()
        return if (clean.isBlank() || clean.equals(AUTO_QUALITY, ignoreCase = true)) BEST_QUALITY else clean
    }

    private fun sanitizeFileName(value: String): String =
        value.replace(Regex("""[\\/:*?"<>|]+"""), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(MAX_FILE_NAME_LENGTH)
            .ifBlank { DEFAULT_VIDEO_NAME }

    private companion object {
        const val AUTO_QUALITY = "auto"
        const val BEST_QUALITY = "best"
        const val DEFAULT_VIDEO_EXTENSION = "mp4"
        const val DEFAULT_VIDEO_NAME = "TypeType video"
        const val JOB_POLL_DELAY_MS = 1_000L
        const val MAX_JOB_POLLS = 900
        const val MAX_FILE_NAME_LENGTH = 180
        const val MAX_STORED_DOWNLOADS = 80
        const val PREFS_NAME = "typetype_downloads"
        const val KEY_DOWNLOADS = "downloads"
    }
}
