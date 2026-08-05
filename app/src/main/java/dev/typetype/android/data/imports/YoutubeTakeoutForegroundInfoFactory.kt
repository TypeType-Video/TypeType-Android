package dev.typetype.android.data.imports

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import dev.typetype.android.R
import java.util.UUID

internal class YoutubeTakeoutForegroundInfoFactory(
    private val context: Context,
) {
    fun create(workId: UUID, fileName: String, status: String, progress: Int?): ForegroundInfo {
        ensureChannel()
        val phase = context.getString(status.phaseLabel())
        val contentIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { intent ->
            PendingIntent.getActivity(
                context,
                workId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(context.getString(R.string.youtube_takeout_notification_title))
            .setContentText(context.getString(R.string.youtube_takeout_notification_progress, fileName, phase))
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(100, progress ?: 0, progress == null)
            .build()
        return ForegroundInfo(workId.hashCode(), notification, foregroundServiceType())
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.youtube_takeout_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private fun String.phaseLabel(): Int = when (this) {
        "UPLOADING" -> R.string.settings_import_youtube_uploading
        "PARSING" -> R.string.settings_import_youtube_parsing
        "IMPORTING" -> R.string.settings_import_youtube_importing
        else -> R.string.settings_import_youtube_queued
    }

    private fun foregroundServiceType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
    } else {
        0
    }

    private companion object {
        const val CHANNEL_ID = "youtube_takeout_imports"
    }
}
