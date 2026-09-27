package dev.typetype.android.services.push

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.R
import dev.typetype.android.domain.navigation.toPublicWatchParameter
import dev.typetype.android.domain.notifications.NotificationItem
import dev.typetype.android.domain.push.PushPayload
import dev.typetype.android.domain.push.parsePushPayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun canNotify(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun notify(payload: PushPayload) {
        show(
            title = payload.channelName.ifBlank { payload.serviceName },
            text = payload.title,
            videoUrl = payload.videoUrl,
            notificationId = payload.eventId.hashCode(),
        )
    }

    fun notifySubscription(item: NotificationItem) {
        show(
            title = item.channelName.ifBlank { item.video.uploaderName },
            text = item.title,
            videoUrl = item.video.url,
            notificationId = item.video.id.hashCode(),
        )
    }

    private fun show(title: String, text: String, videoUrl: String, notificationId: Int) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(notificationChannel())
        }
        manager.notify(notificationId, buildNotification(title, text, videoUrl, notificationId))
    }

    private fun buildNotification(
        title: String,
        text: String,
        videoUrl: String,
        notificationId: Int,
    ): Notification {
        val deepLink = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("typetype://watch?v=${toPublicWatchParameter(videoUrl)}"),
        )
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            deepLink,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun notificationChannel(): NotificationChannel = NotificationChannel(
        CHANNEL_ID,
        context.getString(R.string.push_notification_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT,
    )

    private companion object {
        const val CHANNEL_ID = "subscription_push"
    }
}
