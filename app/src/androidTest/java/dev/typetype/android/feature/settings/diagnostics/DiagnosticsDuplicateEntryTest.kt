package dev.typetype.android.feature.settings.diagnostics

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
}
