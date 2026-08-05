package dev.typetype.android.feature.settings.about

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.typetype.android.domain.version.ComponentVersion
import dev.typetype.android.domain.version.ComponentVersions
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AboutVersionsSectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun displaysEachComponentIndependentlyAndRefreshes() {
        var refreshes = 0
        composeRule.setContent {
            MaterialTheme {
                AboutVersionsSection(
                    versions = ComponentVersions(
                        frontend = version("web", "frontend-revision"),
                        server = version("server", "server-revision"),
                    ),
                    isLoading = false,
                    onRefresh = { refreshes += 1 },
                )
            }
        }

        composeRule.onNodeWithText("frontend-revision").assertIsDisplayed()
        composeRule.onNodeWithText("server-revision").assertIsDisplayed()
        composeRule.onAllNodesWithText("Unavailable").assertCountEquals(2)
        composeRule.onNodeWithContentDescription("Refresh versions").performClick()

        composeRule.runOnIdle { assertEquals(1, refreshes) }
    }

    @Test
    fun disablesRefreshWhileLoading() {
        composeRule.setContent {
            MaterialTheme {
                AboutVersionsSection(
                    versions = null,
                    isLoading = true,
                    onRefresh = {},
                )
            }
        }

        composeRule.onAllNodesWithText("Loading…").assertCountEquals(4)
        composeRule.onNodeWithContentDescription("Refresh versions").assertIsNotEnabled()
    }

    private fun version(service: String, revision: String) = ComponentVersion(
        service = service,
        version = "1.3.1",
        revision = revision,
        shortRevision = revision.take(7),
        buildTime = "2026-08-05T10:54:40Z",
    )
}
