package dev.typetype.android.data.feed

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.typetype.android.data.account.AccountEntity
import dev.typetype.android.data.database.TypeTypeDatabase
import dev.typetype.android.data.server.ServerEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubscriptionFeedCachePersistenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private var database: TypeTypeDatabase? = null

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun cachedFeedSurvivesRecreationAndRemainsScoped() = runBlocking {
        openDatabase().also { current ->
            seedAccount(current, SERVER_A, ACCOUNT_A)
            seedAccount(current, SERVER_A, ACCOUNT_B)
            seedAccount(current, SERVER_B, ACCOUNT_A)
            current.feedVideoDao().replace(
                SERVER_A,
                ACCOUNT_A,
                SUBSCRIPTIONS,
                listOf(feedRow(SERVER_A, ACCOUNT_A, "video-a")),
            )
            current.close()
            database = null
        }

        val recreated = openDatabase()
        val restored = recreated.feedVideoDao().get(SERVER_A, ACCOUNT_A, SUBSCRIPTIONS)

        assertEquals(listOf("video-a"), restored.map { it.videoId })
        assertTrue(recreated.feedVideoDao().get(SERVER_A, ACCOUNT_B, SUBSCRIPTIONS).isEmpty())
        assertTrue(recreated.feedVideoDao().get(SERVER_B, ACCOUNT_A, SUBSCRIPTIONS).isEmpty())
        assertTrue(recreated.feedVideoDao().get(SERVER_A, ACCOUNT_A, "home").isEmpty())
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
            ServerEntity(serverId, "https://$serverId.example/api/", serverId, 1L),
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

    private fun feedRow(serverId: String, accountId: String, videoId: String) = FeedVideoEntity(
        serverId = serverId,
        accountId = accountId,
        feed = SUBSCRIPTIONS,
        position = 0,
        videoUrl = "https://video.example/watch?v=$videoId",
        videoId = videoId,
        title = "Subscription video",
        thumbnailUrl = "https://image.example/$videoId",
        uploaderName = "Channel",
        uploaderUrl = "https://video.example/channel",
        uploaderAvatarUrl = "",
        uploaderVerified = false,
        durationSeconds = 120,
        isLive = false,
        viewCount = 10,
        uploadedAtMillis = 20,
        isShortFormContent = false,
        shortDescription = null,
        publishedAtMillis = 20,
        isPostLive = false,
        isLiveContent = false,
        requiresMembership = false,
        savedAtMillis = 30,
    )

    private companion object {
        const val DATABASE_NAME = "subscription-feed-cache-persistence.db"
        const val SERVER_A = "server-a"
        const val SERVER_B = "server-b"
        const val ACCOUNT_A = "account-a"
        const val ACCOUNT_B = "account-b"
        const val SUBSCRIPTIONS = "subscriptions"
    }
}
