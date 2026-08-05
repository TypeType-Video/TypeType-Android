package dev.typetype.android.feature.settings.imports

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
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
                selectedPipePipeDocument = ImportDocument(
                    uri = "content://backup",
                    displayName = "backup.zip",
                    sizeBytes = 1024L,
                    mediaType = "application/zip",
                ),
            ),
        )

        scrollTo("backup.zip")
        composeRule.onNodeWithText("backup.zip").assertIsDisplayed()
        scrollTo("Restore backup")
        composeRule.onNodeWithText("Restore backup").assertIsEnabled()
    }

    @Test
    fun completedRestoreShowsImportedCounts() {
        setScreen(
            ImportDataState(
                pipePipeSummary = PipePipeRestoreSummary(
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

        scrollTo("Import complete")
        composeRule.onNodeWithText("Import complete").assertIsDisplayed()
        scrollTo("60")
        composeRule.onNodeWithText("60").assertIsDisplayed()
    }

    private fun setScreen(state: ImportDataState) {
        composeRule.setContent {
            TypeTypeTheme {
                ImportDataScreen(
                    state = state,
                    youtubeState = YoutubeTakeoutImportState(),
                    onNavigateBack = {},
                    onToggleCategory = {},
                    onExportTypeType = {},
                    onChooseTypeTypeBackup = {},
                    onRestoreTypeType = {},
                    onDismissTypeTypeRestore = {},
                    onResetTypeTypeResult = {},
                    onChoosePipePipeBackup = {},
                    onRestorePipePipe = {},
                    onResetPipePipeResult = {},
                    onOpenYoutubeTakeout = {},
                    onChooseYoutubeTakeout = {},
                    onRetryYoutubeTakeout = {},
                    onCancelYoutubeTakeout = {},
                    onRemoveYoutubeTakeout = {},
                    onRetryYoutubeRefresh = {},
                )
            }
        }
    }

    private fun scrollTo(text: String) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
    }
}
