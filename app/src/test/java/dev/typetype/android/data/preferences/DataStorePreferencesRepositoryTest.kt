package dev.typetype.android.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
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

class DataStorePreferencesRepositoryTest {
    @Test
    fun `seek interval and codec survive repository recreation`() = runBlocking {
        val directory = Files.createTempDirectory("player-preferences").toFile()
        val file = File(directory, "player.preferences_pb")
        val firstJob = Job()
        val firstScope = CoroutineScope(Dispatchers.IO + firstJob)
        val first = DataStorePreferencesRepository(
            PreferenceDataStoreFactory.create(scope = firstScope) { file },
        )
        first.setPlayerDoubleTapSeekSeconds(5)
        first.setPlayerPreferredCodec("vp9")
        first.setPlayerAccessibleControlsEnabled(true)
        firstJob.cancelAndJoin()

        val secondJob = Job()
        val secondScope = CoroutineScope(Dispatchers.IO + secondJob)
        val recreated = DataStorePreferencesRepository(
            PreferenceDataStoreFactory.create(scope = secondScope) { file },
        )
        val preferences = recreated.observe().first()

        assertEquals(5, preferences.playerDoubleTapSeekSeconds)
        assertEquals("vp9", preferences.playerPreferredCodec)
        assertEquals(true, preferences.playerAccessibleControlsEnabled)
        secondJob.cancelAndJoin()
        directory.deleteRecursively()
        Unit
    }
}
