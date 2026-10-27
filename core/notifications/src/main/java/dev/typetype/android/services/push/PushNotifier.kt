package dev.typetype.android.services.push

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.graphics.Bitmap
import dev.typetype.android.data.account.AccountScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.typetype.android.R
import dev.typetype.android.domain.navigation.toPublicWatchParameter
import dev.typetype.android.core.error.suspendRunCatching
import dev.typetype.android.domain.notifications.NotificationItem
import dev.typetype.android.domain.notifications.NotificationsRepository
import dev.typetype.android.domain.push.PushPayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val thumbnails: NotificationThumbnails,
    private val notifications: NotificationsRepository,
) {
    private val background = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun canNotify(): Boolean = blockedReason() == null

    fun blockedReason(): NotificationPostResult? = when {
        !NotificationManagerCompat.from(context).areNotificationsEnabled() ->
            NotificationPostResult.NotificationsDisabled
        isChannelBlocked() -> NotificationPostResult.ChannelBlocked
        else -> null
    }

    fun notify(scope: AccountScope, payload: PushPayload): NotificationPostResult {
        val id = subscriptionNotificationId(
            videoId = payload.videoId,
            videoUrl = payload.videoUrl,
            fallback = payload.eventId,
        )
        val alert = VideoAlert(payload.title, payload.channelName, payload.serviceId, payload.videoUrl)
        val result = post(id) { videoNotification(alert, id, thumbnail = null) }
        if (result == NotificationPostResult.Posted) {
            background.launch {
                attachThumbnail(scope, alert, id, pushThumbnailUrl(payload), NO_DELAY + THUMBNAIL_RETRY_DELAYS_MS)
            }
        }
        return result
    }

    suspend fun prepareSubscription(scope: AccountScope, item: NotificationItem): PreparedAlert {
        val alert = VideoAlert(
            videoTitle = item.video.title.ifBlank { item.title },
            channelName = item.channelName.ifBlank { item.video.uploaderName },
            serviceId = item.serviceId,
            videoUrl = item.video.url,
        )
        return PreparedAlert(
            item = item,
            scope = scope,
            id = subscriptionNotificationId(item.video.id, item.video.url, item.video.url),
            alert = alert,
            thumbnail = thumbnails.load(scope, item.video.thumbnailUrl),
        )
    }

    fun postSubscription(prepared: PreparedAlert): NotificationPostResult {
        val result = post(prepared.id) { videoNotification(prepared.alert, prepared.id, prepared.thumbnail) }
        if (result == NotificationPostResult.Posted && prepared.thumbnail == null) {
            background.launch {
                attachThumbnail(
                    prepared.scope,
                    prepared.alert,
                    prepared.id,
                    prepared.item.video.thumbnailUrl,
                    THUMBNAIL_RETRY_DELAYS_MS,
                )
            }
        }
        return result
    }

    private suspend fun attachThumbnail(
        scope: AccountScope,
        alert: VideoAlert,
        id: Int,
        thumbnailUrl: String,
        delaysMs: List<Long>,
    ) {
        if (thumbnailUrl.isBlank()) return
        for (delayMs in delaysMs) {
            delay(delayMs)
            if (!isShowing(id)) return
            val thumbnail = thumbnails.load(scope, thumbnailUrl) ?: continue
            post(id) { videoNotification(alert, id, thumbnail) }
            return
        }
    }

    private suspend fun pushThumbnailUrl(payload: PushPayload): String =
        payload.thumbnailUrl.takeIf { it.isNotBlank() } ?: suspendRunCatching {
            notifications.page(0).getOrThrow().items
                .firstOrNull { it.video.id == payload.videoId || it.video.url == payload.videoUrl }
                ?.video?.thumbnailUrl
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: youtubeThumbnailUrl(payload.serviceId, payload.videoId)

    private fun isShowing(id: Int): Boolean =
        context.getSystemService(NotificationManager::class.java)
            ?.activeNotifications
            ?.any { it.id == id }
            ?: false

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

    private fun videoNotification(alert: VideoAlert, id: Int, thumbnail: Bitmap?): Notification {
        val deepLink = Intent(
            Intent.ACTION_VIEW,
            "typetype://watch?v=${toPublicWatchParameter(alert.videoUrl)}".toUri(),
        )
        val platform = NotificationPlatform.of(alert.serviceId)
        val style = if (thumbnail != null) {
            NotificationCompat.BigPictureStyle()
                .bigPicture(thumbnail)
                .bigLargeIcon(null as Bitmap?)
                .setSummaryText(alert.channelName)
        } else {
            NotificationCompat.BigTextStyle().bigText(alert.videoTitle).setSummaryText(alert.channelName)
        }
        return baseBuilder(PendingIntent.getActivity(context, id, deepLink, PENDING_FLAGS))
            .setContentTitle(alert.videoTitle)
            .setContentText(alert.channelName)
            .setStyle(style)
            .setLargeIcon(thumbnail?.withPlatformBadge(context, platform) ?: platform?.badge(context))
            .apply {
                if (platform != null) {
                    setSubText(context.getString(platform.nameRes))
                    setColor(platform.color)
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
        val NO_DELAY = listOf(0L)
        val THUMBNAIL_RETRY_DELAYS_MS = listOf(90_000L, 240_000L, 600_000L)
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

class PreparedAlert internal constructor(
    val item: NotificationItem,
    internal val scope: AccountScope,
    internal val id: Int,
    internal val alert: VideoAlert,
    internal val thumbnail: Bitmap?,
)

internal data class VideoAlert(
    val videoTitle: String,
    val channelName: String,
    val serviceId: Int,
    val videoUrl: String,
)
