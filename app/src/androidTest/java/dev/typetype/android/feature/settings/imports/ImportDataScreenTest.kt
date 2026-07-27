package dev.typetype.android.feature.settings.imports

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.imports.ImportDocument
import dev.typetype.android.domain.imports.PipePipeRestoreSummary
import org.junit.Rule
import org.junit.Test

class ImportDataScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun restoreRequiresASelectedArchive() {
        setScreen(ImportDataState())

        composeRule.onNodeWithText("Restore backup").assertIsNotEnabled()
    }

    @Test
    fun selectedArchiveCanBeRestored() {
        setScreen(
            ImportDataState(
                selectedDocument = ImportDocument(
                    uri = "content://backup",
                    displayName = "backup.zip",
                    sizeBytes = 1024L,
                    mediaType = "application/zip",
                ),
            ),
        )

        composeRule.onNodeWithText("backup.zip").assertIsDisplayed()
        composeRule.onNodeWithText("Restore backup").assertIsEnabled()
    }

    @Test
    fun completedRestoreShowsImportedCounts() {
        setScreen(
            ImportDataState(
                summary = PipePipeRestoreSummary(
                    history = 10,
                    subscriptions = 20,
                    playlists = 30,
                    playlistVideos = 40,
                    progress = 50,
                    searchHistory = 60,
                    historyMinWatchedAt = null,
                    historyMaxWatchedAt = null,
                ),
            ),
        )

        composeRule.onNodeWithText("Import complete").assertIsDisplayed()
        composeRule.onNodeWithText("60").assertIsDisplayed()
    }

    private fun setScreen(state: ImportDataState) {
        composeRule.setContent {
            TypeTypeTheme {
                ImportDataScreen(
                    state = state,
                    onNavigateBack = {},
                    onChooseFile = {},
                    onRestore = {},
                    onReset = {},
                )
            }
        }
    }
}
