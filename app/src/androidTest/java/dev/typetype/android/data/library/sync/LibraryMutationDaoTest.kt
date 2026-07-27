package dev.typetype.android.data.library.sync

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import dev.typetype.android.domain.library.LibraryCollection
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryMutationDaoTest {
    private lateinit var database: TypeTypeDatabase
    private lateinit var dao: LibraryMutationDao

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TypeTypeDatabase::class.java).build()
        database.serverDao().upsert(
            ServerEntity(
                id = SERVER_ID,
                baseUrl = "https://instance.example/api/",
                displayName = "TypeType",
                addedAt = 1L,
            ),
        )
        listOf(ACCOUNT_A, ACCOUNT_B).forEach { accountId ->
            database.accountDao().upsert(
                AccountEntity(
                    serverId = SERVER_ID,
                    accountId = accountId,
                    publicUsername = null,
                    role = null,
                    avatarUrl = null,
                    avatarType = null,
                    avatarCode = null,
                    isGuest = false,
                    lastUsedAt = 1L,
                    sessionGeneration = 4L,
                ),
            )
        }
        dao = database.libraryMutationDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun newestDesiredStateRejectsLateWorkerCompletion() = runBlocking {
        dao.upsert(mutation(accountId = ACCOUNT_A, desiredPresent = true, version = 1L))
        dao.upsert(mutation(accountId = ACCOUNT_A, desiredPresent = false, version = 2L))

        assertEquals(0, dao.deleteIfCurrent(SERVER_ID, ACCOUNT_A, KEY, 1L))
        val current = requireNotNull(dao.get(SERVER_ID, ACCOUNT_A, KEY))
        assertEquals(2L, current.mutationVersion)
        assertFalse(current.desiredPresent)
        assertEquals(1, dao.deleteIfCurrent(SERVER_ID, ACCOUNT_A, KEY, 2L))
        assertNull(dao.get(SERVER_ID, ACCOUNT_A, KEY))
    }

    @Test
    fun permanentFailureCanBeRetriedWithoutCrossingAccountScope() = runBlocking {
        dao.upsert(mutation(accountId = ACCOUNT_A, desiredPresent = true, version = 1L))
        dao.upsert(mutation(accountId = ACCOUNT_B, desiredPresent = true, version = 1L))

        assertEquals(
            1,
            dao.recordAttempt(
                serverId = SERVER_ID,
                accountId = ACCOUNT_A,
                mutationKey = KEY,
                version = 1L,
                state = MUTATION_FAILED,
                attemptedAt = 10L,
                code = "permission_denied",
                status = 403,
                requestId = "request-a",
            ),
        )
        assertEquals(1, dao.retryFailed(SERVER_ID, ACCOUNT_A, COLLECTION, 20L))

        val retried = requireNotNull(dao.get(SERVER_ID, ACCOUNT_A, KEY))
        assertEquals(MUTATION_PENDING, retried.state)
        assertNull(retried.failureCode)
        assertNull(retried.failureStatusCode)
        assertNull(retried.requestId)
        val other = requireNotNull(dao.get(SERVER_ID, ACCOUNT_B, KEY))
        assertEquals(MUTATION_PENDING, other.state)
        assertEquals(1, dao.pending(SERVER_ID, ACCOUNT_A, 4L, 10).size)
        assertEquals(1, dao.pending(SERVER_ID, ACCOUNT_B, 4L, 10).size)
    }

    @Test
    fun staleSessionRowsAreRemovedWithoutTouchingCurrentGeneration() = runBlocking {
        dao.upsert(mutation(accountId = ACCOUNT_A, desiredPresent = true, version = 1L, generation = 3L))
        dao.upsert(
            mutation(
                accountId = ACCOUNT_A,
                desiredPresent = true,
                version = 1L,
                generation = 4L,
                targetId = "video-b",
            ),
        )

        dao.deleteStale(SERVER_ID, ACCOUNT_A, 4L)

        assertNull(dao.get(SERVER_ID, ACCOUNT_A, KEY))
        assertTrue(dao.pending(SERVER_ID, ACCOUNT_A, 4L, 10).single().targetId == "video-b")
    }

    @Test
    fun pendingIntentIsReappliedAfterRemoteCacheReplacement() = runBlocking {
        val entry = mutation(accountId = ACCOUNT_A, desiredPresent = true, version = 1L)
        dao.upsert(entry)
        val overlay = LibraryMutationOverlay(
            accountDao = database.accountDao(),
            mutationDao = dao,
            favoritesDao = database.favoritesDao(),
            watchLaterDao = database.watchLaterDao(),
            playlistsDao = database.playlistsDao(),
            subscriptionDao = database.subscriptionDao(),
        )

        database.favoritesDao().replaceAll(SERVER_ID, ACCOUNT_A, emptyList())
        overlay.apply(entry.scope(), LibraryCollection.Favorites)

        val favorite = database.favoritesDao().getAll(SERVER_ID, ACCOUNT_A).single()
        assertEquals(TARGET, favorite.videoUrl)
        assertEquals("Video", favorite.title)

        dao.upsert(entry.copy(desiredPresent = false, mutationVersion = 2L, updatedAtMillis = 2L))
        overlay.apply(entry.scope(), LibraryCollection.Favorites)
        assertTrue(database.favoritesDao().getAll(SERVER_ID, ACCOUNT_A).isEmpty())
    }

    private fun mutation(
        accountId: String,
        desiredPresent: Boolean,
        version: Long,
        generation: Long = 4L,
        targetId: String = TARGET,
    ): LibraryMutationEntity = LibraryMutationEntity(
        serverId = SERVER_ID,
        accountId = accountId,
        mutationKey = libraryMutationKey(LibraryMutationKind.Favorite, null, targetId),
        collection = COLLECTION,
        kind = LibraryMutationKind.Favorite.storageKey,
        targetId = targetId,
        parentId = null,
        desiredPresent = desiredPresent,
        title = "Video",
        thumbnailUrl = "",
        durationSeconds = 10L,
        channelName = "",
        channelUrl = "",
        channelAvatarUrl = "",
        viewCount = 0L,
        sessionGeneration = generation,
        mutationVersion = version,
        state = MUTATION_PENDING,
        createdAtMillis = 1L,
        updatedAtMillis = version,
        lastAttemptAtMillis = null,
        attemptCount = 0,
        failureCode = null,
        failureStatusCode = null,
        requestId = null,
    )

    private companion object {
        const val SERVER_ID = "server-a"
        const val ACCOUNT_A = "account-a"
        const val ACCOUNT_B = "account-b"
        const val COLLECTION = "favorites"
        const val TARGET = "video-a"
        val KEY = libraryMutationKey(LibraryMutationKind.Favorite, null, TARGET)
    }
}
