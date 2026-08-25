package dev.typetype.android.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dev.typetype.android.domain.preferences.AppearanceFont
import dev.typetype.android.domain.preferences.AppearanceMode
import dev.typetype.android.domain.preferences.AppearanceMotion
import dev.typetype.android.domain.preferences.AppearancePersonality
import dev.typetype.android.domain.preferences.MangaHeadlineMarker
import dev.typetype.android.domain.preferences.MangaPaper
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
    fun `appearance choices survive repository recreation`() = runBlocking {
        val directory = Files.createTempDirectory("appearance-preferences").toFile()
        val file = File(directory, "appearance.preferences_pb")
        val firstJob = Job()
        val first = DataStorePreferencesRepository(
            PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + firstJob)) { file },
        )
        first.setAppearancePersonality(AppearancePersonality.Manga)
        first.setAppearanceMode(AppearanceMode.Dark)
        first.setAppearanceAmoled(true)
        first.setAppearanceFont(AppearanceFont.Expressive)
        first.setAppearanceMotion(AppearanceMotion.Off)
        first.setMangaPaper(MangaPaper.Nord)
        first.setMangaHeadlineMarker(MangaHeadlineMarker.SpeedLines)
        first.setMangaPanelTilt(true)
        firstJob.cancelAndJoin()

        val secondJob = Job()
        val recreated = DataStorePreferencesRepository(
            PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + secondJob)) { file },
        )
        val preferences = recreated.observe().first()

        assertEquals(AppearancePersonality.Manga, preferences.appearancePersonality)
        assertEquals(AppearanceMode.Dark, preferences.appearanceMode)
        assertEquals(true, preferences.appearanceAmoled)
        assertEquals(AppearanceFont.Expressive, preferences.appearanceFont)
        assertEquals(AppearanceMotion.Off, preferences.appearanceMotion)
        assertEquals(MangaPaper.Nord, preferences.mangaPaper)
        assertEquals(MangaHeadlineMarker.SpeedLines, preferences.mangaHeadlineMarker)
        assertEquals(true, preferences.mangaPanelTilt)
        secondJob.cancelAndJoin()
        directory.deleteRecursively()
        Unit
    }

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
