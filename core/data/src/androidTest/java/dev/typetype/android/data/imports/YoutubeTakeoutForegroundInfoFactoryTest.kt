package dev.typetype.android.data.imports

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.typetype.android.R
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class YoutubeTakeoutForegroundInfoFactoryTest {
    @Test
    fun uploadCreatesADataSyncProgressNotification() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val info = YoutubeTakeoutForegroundInfoFactory(context).create(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            "takeout.zip",
            "UPLOADING",
            null,
        )

        assertEquals(
            context.getString(R.string.youtube_takeout_notification_title),
            info.notification.extras.getString("android.title"),
        )
        assertEquals(
            context.getString(
                R.string.youtube_takeout_notification_progress,
                "takeout.zip",
                context.getString(R.string.settings_import_youtube_uploading),
            ),
            info.notification.extras.getString("android.text"),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC, info.foregroundServiceType)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            assertNotNull(manager.getNotificationChannel("youtube_takeout_imports"))
        }
    }
}
