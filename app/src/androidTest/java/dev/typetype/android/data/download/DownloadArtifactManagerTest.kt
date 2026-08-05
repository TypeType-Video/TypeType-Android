package dev.typetype.android.data.download

import android.app.DownloadManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.typetype.android.domain.download.DownloadStatus
import java.util.concurrent.TimeUnit
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
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

    @Test
    fun completedArtifactIsReconciledByANewManagerInstance() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "video/mp4")
                .setHeader("Content-Disposition", "attachment; filename=artifact.mp4")
                .setBody("artifact".repeat(1_024)),
        )
        server.start()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val artifactManager = DownloadArtifactManager(context)
        val systemId = artifactManager.enqueue(
            baseUrl = server.url("/").toString(),
            serverJobId = "completed-job",
            fileName = "typetype-completed-artifact-${System.nanoTime()}.mp4",
            title = "Completed TypeType artifact",
        )

        try {
            assertTrue(waitUntilDownloaded { artifactManager.status(systemId).first })
            val restoredStatus = DownloadArtifactManager(context).status(systemId)

            assertEquals(DownloadStatus.Successful, restoredStatus.first)
            assertEquals(100, restoredStatus.second)
            assertEquals(
                "/downloader/jobs/completed-job/artifact?download=1",
                server.takeRequest(5, TimeUnit.SECONDS)?.path,
            )
        } finally {
            artifactManager.remove(systemId)
            server.shutdown()
        }
    }

    private fun waitUntilDownloaded(status: () -> DownloadStatus): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20)
        while (System.nanoTime() < deadline) {
            when (status()) {
                DownloadStatus.Successful -> return true
                DownloadStatus.Failed, DownloadStatus.Cancelled -> return false
                else -> Thread.sleep(100)
            }
        }
        return status() == DownloadStatus.Successful
    }
}
