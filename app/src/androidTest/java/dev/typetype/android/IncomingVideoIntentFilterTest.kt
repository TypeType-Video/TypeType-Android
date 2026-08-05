package dev.typetype.android

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IncomingVideoIntentFilterTest {

    @Test
    fun videoLinksAndSharedTextResolveToMainActivity() {
        assertHandled(Intent(Intent.ACTION_VIEW, Uri.parse("typetype://watch?v=sm9")))
        assertHandled(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://beta.typetype.video/watch?v=dQw4w9WgXcQ"),
            ),
        )
        assertHandled(Intent(Intent.ACTION_VIEW, Uri.parse("https://youtu.be/dQw4w9WgXcQ")))
        assertHandled(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.nicovideo.jp/watch/sm9")))
        assertHandled(Intent(Intent.ACTION_VIEW, Uri.parse("https://b23.tv/example")))
        assertHandled(
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "https://youtu.be/dQw4w9WgXcQ"),
        )
    }

    private fun assertHandled(intent: Intent) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val matches = context.packageManager.queryIntentActivities(
            intent.setPackage(context.packageName),
            0,
        )
        assertTrue(matches.any { it.activityInfo.name == MainActivity::class.java.name })
    }
}
