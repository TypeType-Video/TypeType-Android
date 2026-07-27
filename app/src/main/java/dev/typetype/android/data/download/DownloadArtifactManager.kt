package dev.typetype.android.data.download

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.core.net.toUri
import dev.typetype.android.domain.download.DownloadStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadArtifactManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun enqueue(
        baseUrl: String,
        serverJobId: String,
        fileName: String,
        title: String,
    ): Long {
        val url = "${baseUrl.trimEnd('/')}/downloader/jobs/$serverJobId/artifact?download=1"
        val request = DownloadManager.Request(url.toUri())
            .setTitle(title.ifBlank { fileName })
            .setDescription(fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        } else {
            request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
        }
        return manager().enqueue(request)
    }

    fun status(downloadId: Long): Pair<DownloadStatus, Int?> =
        manager().query(DownloadManager.Query().setFilterById(downloadId))?.use { cursor ->
            if (!cursor.moveToFirst()) {
                DownloadStatus.Failed to null
            } else {
                DownloadManagerCursorReader.status(
                    cursor = cursor,
                    statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS),
                    bytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR),
                    totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES),
                )
            }
        } ?: (DownloadStatus.Failed to null)

    fun open(downloadId: Long) {
        val uri = manager().getUriForDownloadedFile(downloadId) ?: error("Download is not ready")
        val intent = Intent(Intent.ACTION_VIEW)
            .setData(uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun remove(downloadId: Long) {
        manager().remove(downloadId)
    }

    private fun manager(): DownloadManager =
        context.getSystemService(DownloadManager::class.java) ?: error("Download manager unavailable")
}
