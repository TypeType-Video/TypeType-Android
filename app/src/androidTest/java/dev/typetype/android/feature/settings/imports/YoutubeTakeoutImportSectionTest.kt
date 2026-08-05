package dev.typetype.android.feature.settings.imports

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.imports.YoutubeTakeoutCategoryCounts
import dev.typetype.android.domain.imports.YoutubeTakeoutImportItem
import dev.typetype.android.domain.imports.YoutubeTakeoutImportStatus
import org.junit.Rule
import org.junit.Test

class YoutubeTakeoutImportSectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyQueueExplainsHowToStart() {
        setSection(YoutubeTakeoutImportState())

        composeRule.onNodeWithText("Open Google Takeout").assertIsDisplayed()
        composeRule.onNodeWithText("Choose Takeout ZIP files").assertIsDisplayed()
        composeRule.onNodeWithText("No Takeout imports yet").assertIsDisplayed()
    }

    @Test
    fun activeImportShowsArchivePreview() {
        setSection(
            YoutubeTakeoutImportState(
                items = listOf(
                    item(
                        status = YoutubeTakeoutImportStatus.Importing,
                        preview = YoutubeTakeoutCategoryCounts(7, 3, 19, 5, 11, 41),
                    ),
                ),
            ),
        )

        composeRule.onNodeWithText("takeout-1.zip").assertIsDisplayed()
        composeRule.onNodeWithText("Importing").assertIsDisplayed()
        composeRule.onNodeWithText("Found in archive").assertIsDisplayed()
        composeRule.onNodeWithText("41").assertIsDisplayed()
    }

    @Test
    fun completedImportShowsReportTotals() {
        setSection(
            YoutubeTakeoutImportState(
                items = listOf(
                    item(
                        status = YoutubeTakeoutImportStatus.Completed,
                        imported = 137,
                        skipped = 23,
                        failed = 2,
                    ),
                ),
            ),
        )

        composeRule.onNodeWithText("Import complete").assertIsDisplayed()
        composeRule.onNodeWithText("137").assertIsDisplayed()
        composeRule.onNodeWithText("23").assertIsDisplayed()
    }

    @Test
    fun failedImportShowsActionableFailure() {
        setSection(
            YoutubeTakeoutImportState(
                items = listOf(
                    item(
                        status = YoutubeTakeoutImportStatus.Failed,
                        failureCode = "YOUTUBE_IMPORT_PERMISSION",
                        requestId = "req-safe-123",
                    ),
                ),
            ),
        )

        composeRule.onNodeWithText(
            "TypeType can no longer read this archive. Choose it again.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed()
        composeRule.onNodeWithText("Remove").assertIsDisplayed()
    }

    private fun setSection(state: YoutubeTakeoutImportState) {
        composeRule.setContent {
            TypeTypeTheme {
                YoutubeTakeoutImportSection(
                    state = state,
                    onOpenTakeout = {},
                    onChooseArchives = {},
                    onRetry = {},
                    onCancel = {},
                    onRemove = {},
                    onRetryCollectionRefresh = {},
                )
            }
        }
    }

    private fun item(
        status: YoutubeTakeoutImportStatus,
        preview: YoutubeTakeoutCategoryCounts? = null,
        imported: Int? = null,
        skipped: Int? = null,
        failed: Int? = null,
        failureCode: String? = null,
        requestId: String? = null,
    ) = YoutubeTakeoutImportItem(
        requestId = "local-request",
        displayName = "takeout-1.zip",
        sizeBytes = 4096,
        status = status,
        progressPercent = 58,
        preview = preview,
        importedCount = imported,
        skippedCount = skipped,
        failedCount = failed,
        warningCount = 0,
        errorCount = 0,
        failureCode = failureCode,
        failureRequestId = requestId,
        needsCollectionRefresh = false,
        createdAtMillis = 1,
    )
}
