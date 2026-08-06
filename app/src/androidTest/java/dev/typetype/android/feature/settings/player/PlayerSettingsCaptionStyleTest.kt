package dev.typetype.android.feature.settings.player

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.usersettings.CaptionStyles
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlayerSettingsCaptionStyleTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun captionSizeIsDiscoverableAndUpdatesServerStyle() {
        val action = AtomicReference<PlayerSettingsAction>()
        composeRule.setContent {
            TypeTypeTheme {
                PlayerSettingsScreen(
                    state = PlayerSettingsState(),
                    onNavigateBack = {},
                    onAction = action::set,
                )
            }
        }

        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Text size"))
        composeRule.onNodeWithText("Text size").performClick()
        composeRule.onNodeWithText("150%").performClick()

        assertEquals(
            PlayerSettingsAction.SetCaptionStyles(CaptionStyles(fontSize = "150%")),
            action.get(),
        )
    }
}
