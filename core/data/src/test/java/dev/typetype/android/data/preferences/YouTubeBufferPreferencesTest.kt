package dev.typetype.android.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import dev.typetype.android.domain.preferences.YOUTUBE_BUFFER_SECONDS
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

class YouTubeBufferPreferencesTest {
    @Test
    fun choicesSurviveRecreationAndInvalidStoredValuesFallBackSafely() = runBlocking {
        val directory = Files.createTempDirectory("youtube-buffer").toFile()
        val file = File(directory, "preferences.preferences_pb")
        val firstJob = Job()
        val secondJob = Job()
        try {
            val store = PreferenceDataStoreFactory.create(
                scope = CoroutineScope(Dispatchers.IO + firstJob),
            ) { file }
            val repository = DataStorePreferencesRepository(store)
            assertEquals(30, repository.observe().first().playerYouTubeBufferSeconds)
            repository.setPlayerPreferredCodec("vp9")
            for (seconds in YOUTUBE_BUFFER_SECONDS) {
                repository.setPlayerYouTubeBufferSeconds(seconds)
                assertEquals(seconds, repository.observe().first().playerYouTubeBufferSeconds)
            }
            firstJob.cancelAndJoin()
            val recreatedStore = PreferenceDataStoreFactory.create(
                scope = CoroutineScope(Dispatchers.IO + secondJob),
            ) { file }
            val recreated = DataStorePreferencesRepository(recreatedStore)
            assertEquals(120, recreated.observe().first().playerYouTubeBufferSeconds)
            assertEquals("vp9", recreated.observe().first().playerPreferredCodec)
            recreatedStore.edit { it[intPreferencesKey("player_youtube_buffer_seconds")] = Int.MAX_VALUE }
            assertEquals(30, recreated.observe().first().playerYouTubeBufferSeconds)
            recreated.setPlayerYouTubeBufferSeconds(10)
            assertEquals(10, recreated.observe().first().playerYouTubeBufferSeconds)
        } finally {
            firstJob.cancelAndJoin()
            secondJob.cancelAndJoin()
            directory.deleteRecursively()
        }
    }
}
