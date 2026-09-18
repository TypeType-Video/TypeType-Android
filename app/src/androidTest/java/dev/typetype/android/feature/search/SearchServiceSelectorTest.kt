package dev.typetype.android.feature.search

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SearchServiceSelectorTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectingAServiceReportsItsAccountId() {
        var selectedService = -1
        composeRule.setContent {
            TypeTypeTheme {
                SearchServiceSelector(service = 0) { service ->
                    selectedService = service
                }
            }
        }

        composeRule.onNodeWithText("NicoNico").performClick()

        assertEquals(6, selectedService)
    }
}
