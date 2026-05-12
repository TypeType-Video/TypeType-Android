package dev.typetype.android.data.download

import android.app.DownloadManager
import android.database.Cursor
import dev.typetype.android.domain.download.DownloadStatus

internal object DownloadManagerCursorReader {
    fun longOrNull(cursor: Cursor, index: Int): Long? =
        if (index >= 0 && !cursor.isNull(index)) cursor.getLong(index) else null

    fun stringOrNull(cursor: Cursor, index: Int): String? =
        if (index >= 0 && !cursor.isNull(index)) cursor.getString(index) else null

    fun status(
        cursor: Cursor,
        statusIndex: Int,
        bytesIndex: Int,
        totalIndex: Int,
    ): Pair<DownloadStatus, Int?> {
        val status = when (cursor.intOrNull(statusIndex)) {
            DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.Successful
            DownloadManager.STATUS_FAILED -> DownloadStatus.Failed
            DownloadManager.STATUS_RUNNING -> DownloadStatus.Running
            else -> DownloadStatus.Pending
        }
        val downloaded = cursor.longOrZero(bytesIndex)
        val total = cursor.longOrZero(totalIndex)
        val percent = if (total > 0) ((downloaded * 100) / total).toInt().coerceIn(0, 100) else null
        return status to percent
    }

    private fun Cursor.intOrNull(index: Int): Int? =
        if (index >= 0 && !isNull(index)) getInt(index) else null

    private fun Cursor.longOrZero(index: Int): Long =
        if (index >= 0 && !isNull(index)) getLong(index) else 0L
}
