package dev.typetype.android.feature.settings.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlayerSettingsDanmakuTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun bulletCommentPreferenceIsDiscoverableAndActionable() {
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

        composeRule.onNodeWithText("Show bullet comments")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        assertEquals(PlayerSettingsAction.SetDanmakuEnabled(true), action.get())
    }
}
