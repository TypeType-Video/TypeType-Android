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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun canNotify(): Boolean = blockedReason() == null

    fun blockedReason(): NotificationPostResult? = when {
        !NotificationManagerCompat.from(context).areNotificationsEnabled() ->
            NotificationPostResult.NotificationsDisabled
        isChannelBlocked() -> NotificationPostResult.ChannelBlocked
        else -> null
    }

    fun notify(payload: PushPayload): NotificationPostResult = show(
        title = payload.channelName.ifBlank { payload.serviceName },
        text = payload.title,
        videoUrl = payload.videoUrl,
        notificationId = subscriptionNotificationId(
            videoId = payload.videoId,
            videoUrl = payload.videoUrl,
            fallback = payload.eventId,
        ),
    )

    fun notifySubscription(item: NotificationItem): NotificationPostResult = show(
        title = item.channelName.ifBlank { item.video.uploaderName },
        text = item.title,
        videoUrl = item.video.url,
        notificationId = subscriptionNotificationId(
            videoId = item.video.id,
            videoUrl = item.video.url,
            fallback = item.video.url,
        ),
    )

    private fun show(
        title: String,
        text: String,
        videoUrl: String,
        notificationId: Int,
    ): NotificationPostResult {
        val manager = context.getSystemService(NotificationManager::class.java)
            ?: return NotificationPostResult.Failed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(notificationChannel())
        }
        blockedReason()?.let { return it }
        return try {
            manager.notify(notificationId, buildNotification(title, text, videoUrl, notificationId))
            NotificationPostResult.Posted
        } catch (_: SecurityException) {
            NotificationPostResult.PermissionDenied
        }
    }

    private fun isChannelBlocked(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        return manager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE
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
            .setOnlyAlertOnce(true)
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

enum class NotificationPostResult {
    Posted,
    NotificationsDisabled,
    ChannelBlocked,
    PermissionDenied,
    Failed,
}

internal fun subscriptionNotificationId(videoId: String, videoUrl: String, fallback: String): Int =
    videoId.trim().ifBlank { videoUrl.trim() }.ifBlank { fallback.trim() }.hashCode()
