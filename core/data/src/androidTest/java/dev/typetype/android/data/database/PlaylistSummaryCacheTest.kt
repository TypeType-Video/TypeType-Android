package dev.typetype.android.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.typetype.android.data.account.AccountEntity
import dev.typetype.android.data.library.local.PlaylistEntity
import dev.typetype.android.data.library.local.PlaylistVideoEntity
import dev.typetype.android.data.server.ServerEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistSummaryCacheTest {
    private lateinit var database: TypeTypeDatabase

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TypeTypeDatabase::class.java).build()
        database.serverDao().upsert(
            ServerEntity(SERVER_ID, "https://instance.example/api/", "Instance", 1L),
        )
        database.accountDao().upsert(
            AccountEntity(
                serverId = SERVER_ID,
                accountId = ACCOUNT_ID,
                publicUsername = "user",
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

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun summaryRefreshPreservesRetainedDetailsAndRemovesMissingPlaylists() = runBlocking {
        val dao = database.playlistsDao()
        dao.replaceDetail(playlist("kept", 1), listOf(video("kept", "old")))
        dao.replaceDetail(playlist("removed", 1), listOf(video("removed", "gone")))

        dao.replaceSummaries(SERVER_ID, ACCOUNT_ID, listOf(playlist("kept", 7)))

        val retained = dao.observeAllWithVideos(SERVER_ID, ACCOUNT_ID).first().single()
        assertEquals(7, retained.playlist.videoCount)
        assertEquals("old", retained.videos.single().id)
        assertFalse(dao.containsPlaylist(cacheKey("removed")))
        assertNull(dao.findVideoId(cacheKey("removed"), "url-removed"))
    }

    @Test
    fun detailRefreshReplacesOnlyTheSelectedPlaylistVideos() = runBlocking {
        val dao = database.playlistsDao()
        dao.replaceDetail(playlist("first", 1), listOf(video("first", "old")))
        dao.replaceDetail(playlist("second", 1), listOf(video("second", "kept")))

        dao.replaceDetail(playlist("first", 1), listOf(video("first", "new")))

        assertNull(dao.findVideoId(cacheKey("first"), "url-first"))
        assertEquals("new", dao.findVideoId(cacheKey("first"), "new-url-first"))
        assertEquals("kept", dao.findVideoId(cacheKey("second"), "url-second"))
    }

    @Test
    fun optimisticCountAdjustmentNeverBecomesNegative() = runBlocking {
        val dao = database.playlistsDao()
        dao.upsertPlaylist(playlist("saved", 2))

        dao.adjustVideoCount(cacheKey("saved"), 1)
        assertEquals(
            3,
            dao.observeAllWithVideos(SERVER_ID, ACCOUNT_ID).first().single().playlist.videoCount,
        )

        dao.adjustVideoCount(cacheKey("saved"), -10)
        assertEquals(
            0,
            dao.observeAllWithVideos(SERVER_ID, ACCOUNT_ID).first().single().playlist.videoCount,
        )
    }

    private fun playlist(id: String, count: Int) = PlaylistEntity(
        cacheKey = cacheKey(id),
        serverId = SERVER_ID,
        accountId = ACCOUNT_ID,
        id = id,
        name = id,
        description = "",
        createdAtMillis = 1L,
        videoCount = count,
    )

    private fun video(playlistId: String, id: String) = PlaylistVideoEntity(
        playlistCacheKey = cacheKey(playlistId),
        playlistId = playlistId,
        id = id,
        url = if (id == "new") "new-url-$playlistId" else "url-$playlistId",
        title = id,
        thumbnailUrl = "",
        durationSeconds = 1L,
        position = 0,
    )

    private fun cacheKey(playlistId: String) = "$SERVER_ID:$ACCOUNT_ID:$playlistId"

    private companion object {
        const val SERVER_ID = "server"
        const val ACCOUNT_ID = "account"
    }
}
