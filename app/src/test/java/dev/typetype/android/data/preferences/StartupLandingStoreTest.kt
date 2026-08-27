package dev.typetype.android.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dev.typetype.android.data.account.AccountScope
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class StartupLandingStoreTest {
    @Test
    fun `landing page is scoped to server and account`() = runBlocking {
        val (store, job, directory) = store()

        store.setLandingPage(
            AccountScope(serverId = "server", accountId = "account"),
            landingPage = "subscriptions",
        )
        store.setLandingPage(
            AccountScope(serverId = "server", accountId = "other"),
            landingPage = "history",
        )

        assertEquals(
            "subscriptions",
            store.observeLandingPage(
                AccountScope(serverId = "server", accountId = "account"),
            ).first(),
        )
        assertEquals(
            "history",
            store.observeLandingPage(
                AccountScope(serverId = "server", accountId = "other"),
            ).first(),
        )
        job.cancelAndJoin()
        directory.deleteRecursively()
        Unit
    }

    @Test
    fun `blank landing page removes the stored value`() = runBlocking {
        val (store, job, directory) = store()
        val account = AccountScope(serverId = "server", accountId = "account")

        store.setLandingPage(account, "playlists")
        store.setLandingPage(account, " ")

        assertEquals("", store.observeLandingPage(account).first())
        job.cancelAndJoin()
        directory.deleteRecursively()
        Unit
    }

    private fun store(): Triple<StartupLandingStore, Job, File> {
        val directory = Files.createTempDirectory("startup-landing").toFile()
        val file = File(directory, "startup.preferences_pb")
        val job = Job()
        val coroutineScope = CoroutineScope(Dispatchers.IO + job)
        val store = StartupLandingStore(
            PreferenceDataStoreFactory.create(scope = coroutineScope) { file },
        )
        return Triple(store, job, directory)
    }
}
