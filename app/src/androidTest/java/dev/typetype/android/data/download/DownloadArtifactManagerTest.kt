package dev.typetype.android.data.download

import android.app.DownloadManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DownloadArtifactManagerTest {
    @Test
    fun enqueueUsesAStorageDestinationAvailableWithoutGoogleServices() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val artifactManager = DownloadArtifactManager(context)
        val systemId = artifactManager.enqueue(
            baseUrl = "https://example.invalid/api/",
            serverJobId = "test-job",
            fileName = "typetype-storage-test.mp4",
            title = "TypeType storage test",
        )

        assertTrue(systemId > 0L)
        context.getSystemService(DownloadManager::class.java)?.remove(systemId)
    }
}
