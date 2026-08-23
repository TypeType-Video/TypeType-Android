package dev.typetype.android.feature.settings.player

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlayerSettingsPlaybackControlsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun defaultCodecCanBeForced() {
        val action = AtomicReference<PlayerSettingsAction>()
        showSettings(action)

        composeRule.onNodeWithText("Default codec").performClick()
        composeRule.onNodeWithText("AV1").performClick()

        assertEquals(PlayerSettingsAction.SetPreferredCodec("av1"), action.get())
    }

    @Test
    fun doubleTapSeekTimeOffersFiveSeconds() {
        val action = AtomicReference<PlayerSettingsAction>()
        showSettings(action)

        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Double-tap seek time"))
        composeRule.onNodeWithText("Double-tap seek time").performClick()
        composeRule.onNodeWithText("5 seconds").performClick()

        assertEquals(PlayerSettingsAction.SetDoubleTapSeekSeconds(5), action.get())
    }

    private fun showSettings(action: AtomicReference<PlayerSettingsAction>) {
        composeRule.setContent {
            TypeTypeTheme {
                PlayerSettingsScreen(
                    state = PlayerSettingsState(),
                    onNavigateBack = {},
                    onAction = action::set,
                )
            }
        }
    }
}
