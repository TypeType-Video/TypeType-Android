package dev.typetype.android.feature.settings.diagnostics

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.diagnostics.DiagnosticEntry
import org.junit.Rule
import org.junit.Test

class DiagnosticsDuplicateEntryTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun entriesWithTheSameTimestampAndRouteCanRenderTogether() {
        val duplicate = DiagnosticEntry(
            timestampEpochMillis = 1_000L,
            method = "GET",
            route = "/sabr/playback/segment",
            statusCode = 200,
            durationMillis = 10L,
            requestId = null,
        )

        composeRule.setContent {
            TypeTypeTheme {
                LazyColumn {
                    diagnosticRows(listOf(duplicate, duplicate))
                }
            }
        }

        composeRule.onAllNodesWithText("GET /sabr/playback/segment").assertCountEquals(2)
    }

    @Test
    fun duplicateEntriesCanArriveWhileTheListIsBeingScrolled() {
        var entries by mutableStateOf(List(20) { entry(timestamp = it.toLong()) })

        composeRule.setContent {
            TypeTypeTheme {
                LazyColumn(modifier = Modifier.testTag(DIAGNOSTICS_LIST)) {
                    diagnosticRows(entries)
                }
            }
        }

        repeat(25) { batch ->
            composeRule.runOnIdle {
                entries = entries + List(4) { entry(timestamp = 1_000L + batch) }
            }
            composeRule.onNodeWithTag(DIAGNOSTICS_LIST).performScrollToIndex(entries.lastIndex)
        }

        composeRule.waitForIdle()
    }

    private fun entry(timestamp: Long) = DiagnosticEntry(
        timestampEpochMillis = timestamp,
        method = "GET",
        route = "/sabr/playback/segment",
        statusCode = 200,
        durationMillis = 10L,
        requestId = null,
    )

    private companion object {
        const val DIAGNOSTICS_LIST = "diagnostics-list"
    }
}
