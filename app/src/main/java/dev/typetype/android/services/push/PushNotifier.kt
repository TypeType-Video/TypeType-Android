package dev.typetype.android.services.push

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.R
import dev.typetype.android.domain.navigation.toPublicWatchParameter
import dev.typetype.android.domain.push.PushPayload
import dev.typetype.android.domain.push.parsePushPayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun notify(payload: PushPayload) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (!manager.areNotificationsEnabled()) return
        manager.createNotificationChannel(notificationChannel())
        manager.notify(payload.notificationId(), buildNotification(payload))
    }

    private fun buildNotification(payload: PushPayload): Notification {
        val deepLink = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("typetype://watch?v=${toPublicWatchParameter(payload.videoUrl)}"),
        )
        val contentIntent = PendingIntent.getActivity(
            context,
            payload.notificationId(),
            deepLink,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val fallbackTitle = payload.channelName.ifBlank { payload.serviceName }
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(fallbackTitle)
            .setContentText(payload.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.title))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
    }

    private fun notificationChannel(): NotificationChannel = NotificationChannel(
        CHANNEL_ID,
        context.getString(R.string.push_notification_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT,
    )

    private fun PushPayload.notificationId(): Int = eventId.hashCode()

    private companion object {
        const val CHANNEL_ID = "subscription_push"
    }
}
