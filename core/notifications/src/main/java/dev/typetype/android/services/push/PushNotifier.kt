package dev.typetype.android.services.push

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
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

    fun notify(payload: PushPayload): NotificationPostResult = post(
        id = subscriptionNotificationId(
            videoId = payload.videoId,
            videoUrl = payload.videoUrl,
            fallback = payload.eventId,
        ),
    ) { id ->
        videoNotification(
            videoTitle = payload.title,
            channelName = payload.channelName,
            serviceId = payload.serviceId,
            videoUrl = payload.videoUrl,
            id = id,
        )
    }

    fun notifySubscription(item: NotificationItem): NotificationPostResult = post(
        id = subscriptionNotificationId(
            videoId = item.video.id,
            videoUrl = item.video.url,
            fallback = item.video.url,
        ),
    ) { id ->
        videoNotification(
            videoTitle = item.video.title.ifBlank { item.title },
            channelName = item.channelName.ifBlank { item.video.uploaderName },
            serviceId = item.serviceId,
            videoUrl = item.video.url,
            id = id,
        )
    }

    fun notifySummary(items: List<NotificationItem>): NotificationPostResult = post(SUMMARY_ID) { id ->
        summaryNotification(items, id)
    }

    private fun post(id: Int, build: (Int) -> Notification): NotificationPostResult {
        val manager = context.getSystemService(NotificationManager::class.java)
            ?: return NotificationPostResult.Failed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(notificationChannel())
        }
        blockedReason()?.let { return it }
        return try {
            manager.notify(id, build(id))
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

    private fun videoNotification(
        videoTitle: String,
        channelName: String,
        serviceId: Int,
        videoUrl: String,
        id: Int,
    ): Notification {
        val deepLink = Intent(
            Intent.ACTION_VIEW,
            "typetype://watch?v=${toPublicWatchParameter(videoUrl)}".toUri(),
        )
        val platform = NotificationPlatform.of(serviceId)
        return baseBuilder(PendingIntent.getActivity(context, id, deepLink, PENDING_FLAGS))
            .setContentTitle(videoTitle)
            .setContentText(channelName)
            .setStyle(NotificationCompat.BigTextStyle().bigText(videoTitle).setSummaryText(channelName))
            .apply {
                if (platform != null) {
                    setSubText(context.getString(platform.nameRes))
                    setColor(platform.color)
                    setLargeIcon(platform.badge(context))
                }
            }
            .build()
    }

    private fun summaryNotification(items: List<NotificationItem>, id: Int): Notification {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()
        val title = context.resources.getQuantityString(R.plurals.push_summary_title, items.size, items.size)
        val style = NotificationCompat.InboxStyle().setBigContentTitle(title)
        items.take(SUMMARY_LINES).forEach { item ->
            style.addLine("${item.channelName.ifBlank { item.video.uploaderName }} · ${item.video.title}")
        }
        if (items.size > SUMMARY_LINES) {
            val more = items.size - SUMMARY_LINES
            style.setSummaryText(context.resources.getQuantityString(R.plurals.push_summary_more, more, more))
        }
        return baseBuilder(PendingIntent.getActivity(context, id, launch, PENDING_FLAGS))
            .setContentTitle(title)
            .setContentText(items.map { it.channelName }.distinct().joinToString(", "))
            .setStyle(style)
            .setNumber(items.size)
            .build()
    }

    private fun baseBuilder(contentIntent: PendingIntent): NotificationCompat.Builder =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)

    @RequiresApi(Build.VERSION_CODES.O)
    private fun notificationChannel(): NotificationChannel = NotificationChannel(
        CHANNEL_ID,
        context.getString(R.string.push_notification_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT,
    )

    private companion object {
        const val CHANNEL_ID = "subscription_push"
        const val SUMMARY_ID = 0x7454
        const val SUMMARY_LINES = 5
        const val PENDING_FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    }
}

enum class NotificationPostResult {
    Posted,
    NotificationsDisabled,
    ChannelBlocked,
    PermissionDenied,
    Failed,
}

fun subscriptionNotificationId(videoId: String, videoUrl: String, fallback: String): Int =
    videoId.trim().ifBlank { videoUrl.trim() }.ifBlank { fallback.trim() }.hashCode()
