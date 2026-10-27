package dev.typetype.android.services.push

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.typetype.android.domain.push.PushPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PushNotifierAndroidTest {
    @Test
    fun subscriptionPayloadBecomesVisibleNotification() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        grantNotificationPermission(context)
        val manager = requireNotNull(context.getSystemService(NotificationManager::class.java))
        val payload = PushPayload(
            eventType = "subscription_new_video",
            serviceId = 0,
            serviceName = "YouTube",
            eventId = "push-notifier-test",
            videoId = "push-notifier-video",
            videoUrl = "https://www.youtube.com/watch?v=push-notifier-video",
            channelId = "channel-test",
            channelName = "Notification test channel",
            channelAvatarUrl = "",
            instanceId = "instance-test",
            accountId = "account-test",
            publishedAt = 1_760_000_000_000,
            title = "Notification delivery smoke test",
        )
        val notificationId = subscriptionNotificationId(payload.videoId, payload.videoUrl, payload.eventId)

        try {
            val result = PushNotifier(context).notify(payload)
            val notification = manager.activeNotifications
                .firstOrNull { record -> record.id == notificationId }
                ?.notification

            assertEquals(NotificationPostResult.Posted, result)
            assertNotNull(notification)
            assertEquals(
                payload.channelName,
                notification?.extras?.getString(Notification.EXTRA_TITLE),
            )
            assertEquals(
                payload.title,
                notification?.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            )
        } finally {
            manager.cancel(notificationId)
        }
    }

    private fun grantNotificationPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
