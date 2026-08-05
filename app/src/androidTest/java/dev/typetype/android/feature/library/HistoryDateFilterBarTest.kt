package dev.typetype.android.feature.library

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HistoryDateFilterBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun todayFilterReportsTheRequestedSelection() {
        val selected = AtomicReference<HistoryDateSelection>()
        val selectedDate = AtomicReference<Long>()
        show(
            onSelectionChange = { selection, date ->
                selected.set(selection)
                selectedDate.set(date)
            },
        )

        composeRule.onNodeWithText("Today").performClick()

        assertEquals(HistoryDateSelection.Today, selected.get())
        assertNull(selectedDate.get())
    }

    @Test
    fun clearActionIsHiddenForAnEmptyHistory() {
        show(canClearHistory = false)

        composeRule.onAllNodesWithText("Clear all").assertCountEquals(0)
    }

    @Test
    fun clearActionRequiresConfirmation() {
        val cleared = AtomicBoolean(false)
        show(
            canClearHistory = true,
            onClearHistory = { cleared.set(true) },
        )

        composeRule.onNodeWithText("Clear all").assertIsEnabled().performClick()
        composeRule.onNodeWithText("Clear watch history?").assertExists()
        composeRule.onAllNodesWithText("Clear all")
            .assertCountEquals(2)[1]
            .assertIsEnabled()
            .performClick()

        assertTrue(cleared.get())
    }

    private fun show(
        canClearHistory: Boolean = true,
        onSelectionChange: (HistoryDateSelection, Long?) -> Unit = { _, _ -> },
        onClearHistory: () -> Unit = {},
    ) {
        composeRule.setContent {
            TypeTypeTheme {
                HistoryDateFilterBar(
                    selection = HistoryDateSelection.All,
                    selectedDateMillis = null,
                    canClearHistory = canClearHistory,
                    onSelectionChange = onSelectionChange,
                    onClearHistory = onClearHistory,
                )
            }
        }
    }
}
