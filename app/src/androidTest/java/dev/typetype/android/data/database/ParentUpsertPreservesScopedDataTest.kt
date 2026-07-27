package dev.typetype.android.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.typetype.android.data.account.AccountEntity
import dev.typetype.android.data.library.local.FavoriteEntity
import dev.typetype.android.data.library.local.PlaylistEntity
import dev.typetype.android.data.library.local.PlaylistVideoEntity
import dev.typetype.android.data.server.ServerEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParentUpsertPreservesScopedDataTest {
    private lateinit var database: TypeTypeDatabase

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TypeTypeDatabase::class.java).build()
        database.serverDao().upsert(server(displayName = "Before"))
        database.accountDao().upsert(account(publicUsername = "before"))
        database.favoritesDao().upsert(favorite())
        database.playlistsDao().upsertPlaylist(playlist(name = "Before"))
        database.playlistsDao().upsertVideos(listOf(playlistVideo()))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun profileRefreshDoesNotCascadeDeleteAccountData() = runBlocking {
        database.accountDao().upsert(account(publicUsername = "after"))

        assertEquals("after", database.accountDao().get(SERVER_ID, ACCOUNT_ID)?.publicUsername)
        assertEquals("Video", database.favoritesDao().getAll(SERVER_ID, ACCOUNT_ID).single().title)
    }

    @Test
    fun instanceRefreshDoesNotCascadeDeleteAccounts() = runBlocking {
        database.serverDao().upsert(server(displayName = "After"))

        assertEquals("After", database.serverDao().getById(SERVER_ID)?.displayName)
        assertEquals(ACCOUNT_ID, database.accountDao().get(SERVER_ID, ACCOUNT_ID)?.accountId)
        assertEquals("Video", database.favoritesDao().getAll(SERVER_ID, ACCOUNT_ID).single().title)
    }

    @Test
    fun playlistMetadataUpdateDoesNotCascadeDeleteVideos() = runBlocking {
        database.playlistsDao().upsertPlaylist(playlist(name = "After"))

        assertEquals(VIDEO_ID, database.playlistsDao().findVideoId(PLAYLIST_CACHE_KEY, VIDEO_URL))
    }

    private fun server(displayName: String) = ServerEntity(
        id = SERVER_ID,
        baseUrl = "https://instance.example/api/",
        displayName = displayName,
        addedAt = 1L,
    )

    private fun account(publicUsername: String) = AccountEntity(
        serverId = SERVER_ID,
        accountId = ACCOUNT_ID,
        publicUsername = publicUsername,
        role = "user",
        avatarUrl = null,
        avatarType = null,
        avatarCode = null,
        isGuest = false,
        lastUsedAt = 2L,
        sessionGeneration = 3L,
    )

    private fun favorite() = FavoriteEntity(
        serverId = SERVER_ID,
        accountId = ACCOUNT_ID,
        videoUrl = "https://video.example/watch?v=1",
        favoritedAtMillis = 4L,
        title = "Video",
        thumbnailUrl = "thumbnail",
        durationSeconds = 5L,
        channelName = "Channel",
        channelUrl = "channel",
        channelAvatarUrl = "avatar",
        viewCount = 6L,
    )

    private fun playlist(name: String) = PlaylistEntity(
        cacheKey = PLAYLIST_CACHE_KEY,
        serverId = SERVER_ID,
        accountId = ACCOUNT_ID,
        id = PLAYLIST_ID,
        name = name,
        description = "",
        createdAtMillis = 7L,
    )

    private fun playlistVideo() = PlaylistVideoEntity(
        playlistCacheKey = PLAYLIST_CACHE_KEY,
        playlistId = PLAYLIST_ID,
        id = VIDEO_ID,
        url = VIDEO_URL,
        title = "Video",
        thumbnailUrl = "thumbnail",
        durationSeconds = 5L,
        position = 0,
    )

    private companion object {
        const val SERVER_ID = "server"
        const val ACCOUNT_ID = "account"
        const val PLAYLIST_ID = "playlist"
        const val PLAYLIST_CACHE_KEY = "playlist-cache"
        const val VIDEO_ID = "video"
        const val VIDEO_URL = "https://video.example/watch?v=1"
    }
}
