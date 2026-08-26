package dev.typetype.android.feature.settings.imports

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.imports.PortabilityCapability
import dev.typetype.android.domain.imports.PortabilityDirection
import dev.typetype.android.domain.imports.PortabilityFidelity
import dev.typetype.android.domain.imports.PortabilityFormat
import dev.typetype.android.domain.imports.TypeTypeBackupCategory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PortabilityPanelTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun exportFormShowsFormatAssetsCategoriesAndTabs() {
        val format = PortabilityFormat(
            format = "typetype",
            adapterVersion = 1,
            capabilities = TypeTypeBackupCategory.entries.map { category ->
                PortabilityCapability(
                    category = category,
                    directions = setOf(PortabilityDirection.Import, PortabilityDirection.Export),
                    fidelity = if (category == TypeTypeBackupCategory.History) {
                        PortabilityFidelity.Partial
                    } else {
                        PortabilityFidelity.Complete
                    },
                )
            },
            defaultExtension = "json",
            contentType = "application/json",
        )

        composeRule.setContent {
            var mode by remember { mutableStateOf(PortabilityScreenMode.Export) }
            val selected = remember { mutableStateOf(setOf(TypeTypeBackupCategory.Subscriptions)) }
            TypeTypeTheme {
                PortabilityPanel(
                    state = PortabilityUiState(
                        mode = mode,
                        formats = listOf(format),
                        selectedFormat = format,
                        selectedCategories = selected.value,
                    ),
                    onModeSelected = { mode = it },
                    onFormatSelected = {},
                    onCategoryToggled = { category ->
                        selected.value = selected.value.toMutableSet().apply {
                            if (!add(category)) remove(category)
                        }
                    },
                    onPolicySelected = {},
                    onStartExport = {},
                    onChooseFile = {},
                    onApplyImport = {},
                    onCancelJob = {},
                    onResetJob = {},
                    onDownloadArtifact = {},
                    onDownloadReport = {},
                )
            }
        }

        composeRule.onNodeWithText("Import").assertIsDisplayed()
        composeRule.onNodeWithText("Export").assertIsDisplayed()
        composeRule.onNodeWithText("Destination format").assertIsDisplayed()
        composeRule.onNodeWithText(".json").assertIsDisplayed()
        composeRule.onNodeWithText("Subscriptions").assertIsDisplayed()
        composeRule.onNodeWithText("Generate export").assertExists()

        composeRule.onNode(hasText("Import")).performClick()
        composeRule.onNodeWithText("Get your TypeType backup").assertIsDisplayed()
        composeRule.onNodeWithText("Choose or drop a backup file").assertIsDisplayed()
    }
}
