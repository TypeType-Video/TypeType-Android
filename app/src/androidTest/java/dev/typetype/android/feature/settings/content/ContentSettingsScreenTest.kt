package dev.typetype.android.feature.settings.content

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ContentSettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun DeArrowOptionsFollowTheServerEnabledPreference() {
        show(ContentSettingsState(isLoading = false, deArrowEnabled = true))

        composeRule.onNodeWithText("Titles").assertIsDisplayed()
        composeRule.onNodeWithText("Thumbnails").assertIsDisplayed()
        composeRule.onNodeWithText("Confidence").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Community, then neutral frame")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun DeArrowToggleReportsTheRequestedServerValue() {
        val action = AtomicReference<ContentSettingsAction>()
        show(ContentSettingsState(isLoading = false)) { action.set(it) }

        composeRule.onNodeWithText("DeArrow titles and thumbnails").performClick()

        assertEquals(ContentSettingsAction.SetDeArrowEnabled(true), action.get())
    }

    @Test
    fun controlsStayDisabledUntilTheServerSettingsLoad() {
        show(ContentSettingsState(isLoading = true))

        composeRule.onAllNodes(isToggleable())[0].assertIsNotEnabled()
    }

    @Test
    fun updateFailureKeepsTheRequestIdReviewable() {
        show(
            ContentSettingsState(
                isLoading = false,
                errorMessage = "The instance could not save this setting",
                errorRequestId = "request-123",
            ),
        )

        composeRule.onNodeWithText("The instance could not save this setting").assertIsDisplayed()
        composeRule.onNodeWithText("Request request-123").assertIsDisplayed()
    }

    private fun show(
        state: ContentSettingsState,
        onAction: (ContentSettingsAction) -> Unit = {},
    ) {
        composeRule.setContent {
            TypeTypeTheme {
                ContentSettingsScreen(
                    state = state,
                    onAction = onAction,
                    onNavigateBack = {},
                )
            }
        }
    }
}
