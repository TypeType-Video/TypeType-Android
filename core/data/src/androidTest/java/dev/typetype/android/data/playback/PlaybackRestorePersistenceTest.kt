package dev.typetype.android.data.playback

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.typetype.android.data.account.AccountEntity
import dev.typetype.android.data.database.TypeTypeDatabase
import dev.typetype.android.data.server.ServerEntity
import dev.typetype.android.domain.playback.PlaybackQueueEntry
import dev.typetype.android.domain.playback.PlaybackQueueSnapshot
import dev.typetype.android.domain.playback.PlaybackRepeatMode
import dev.typetype.android.domain.playback.PlaybackResume
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackRestorePersistenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private var database: TypeTypeDatabase? = null

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun resumeAndQueueSurviveDatabaseRecreationWithinTheirAccountScope() = runBlocking {
        openDatabase().also { current ->
            seedAccount(current, SERVER_ID, ACCOUNT_ID)
            seedAccount(current, OTHER_SERVER_ID, OTHER_ACCOUNT_ID)
            RoomPlaybackResumeRepository(current.playbackResumeDao()).save(resume())
            RoomPlaybackQueueRepository(current.playbackQueueDao()).save(queue())
            current.close()
            database = null
        }

        val recreated = openDatabase()
        val resumeRepository = RoomPlaybackResumeRepository(recreated.playbackResumeDao())
        val queueRepository = RoomPlaybackQueueRepository(recreated.playbackQueueDao())

        assertEquals(resume(), resumeRepository.get(SERVER_ID, ACCOUNT_ID))
        assertEquals(queue(), queueRepository.get(SERVER_ID, ACCOUNT_ID))
        assertNull(resumeRepository.get(OTHER_SERVER_ID, OTHER_ACCOUNT_ID))
        assertNull(queueRepository.get(OTHER_SERVER_ID, OTHER_ACCOUNT_ID))
    }

    private fun openDatabase(): TypeTypeDatabase = Room.databaseBuilder(
        context,
        TypeTypeDatabase::class.java,
        DATABASE_NAME,
    ).build().also { database = it }

    private suspend fun seedAccount(
        target: TypeTypeDatabase,
        serverId: String,
        accountId: String,
    ) {
        target.serverDao().upsert(
            ServerEntity(serverId, "https://instance.example/api/", serverId, 1L),
        )
        target.accountDao().upsert(
            AccountEntity(
                serverId = serverId,
                accountId = accountId,
                publicUsername = accountId,
                role = "user",
                avatarUrl = null,
                avatarType = null,
                avatarCode = null,
                isGuest = false,
                lastUsedAt = 1L,
                sessionGeneration = 1L,
            ),
        )
    }

    private fun resume() = PlaybackResume(
        serverId = SERVER_ID,
        accountId = ACCOUNT_ID,
        videoUrl = FIRST_VIDEO,
        positionMillis = 42_500L,
        updatedAtMillis = 7L,
    )

    private fun queue() = PlaybackQueueSnapshot(
        serverId = SERVER_ID,
        accountId = ACCOUNT_ID,
        title = "Saved queue",
        entries = listOf(
            entry(FIRST_VIDEO, "First"),
            entry("https://video.example/second", "Second"),
        ),
        currentIndex = 0,
        repeatMode = PlaybackRepeatMode.All,
        updatedAtMillis = 7L,
    )

    private fun entry(videoUrl: String, title: String) = PlaybackQueueEntry(
        videoUrl = videoUrl,
        title = title,
        thumbnailUrl = "https://image.example/$title",
        durationSeconds = 120L,
        channelName = "Channel",
    )

    private companion object {
        const val DATABASE_NAME = "playback-restore-persistence.db"
        const val SERVER_ID = "server-a"
        const val ACCOUNT_ID = "account-a"
        const val OTHER_SERVER_ID = "server-b"
        const val OTHER_ACCOUNT_ID = "account-b"
        const val FIRST_VIDEO = "https://video.example/first"
    }
}
