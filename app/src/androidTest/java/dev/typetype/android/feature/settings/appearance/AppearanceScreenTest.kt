package dev.typetype.android.feature.settings.appearance

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performClick
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.preferences.AccentColor
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppearanceScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun accentCellsExposeTheirLabelSelectionAndAction() {
        val selected = AtomicReference<AccentColor>()
        composeRule.setContent {
            TypeTypeTheme {
                AppearanceScreen(
                    currentAccent = AccentColor.Blue,
                    onAccentSelected = selected::set,
                    onNavigateBack = {},
                )
            }
        }

        composeRule.onNode(isSelectable() and hasText("Blue"))
            .assertIsSelected()
            .assertHasClickAction()
        composeRule.onNode(isSelectable() and hasText("Red"))
            .assertIsNotSelected()
            .performClick()

        assertEquals(AccentColor.Red, selected.get())
    }
}
