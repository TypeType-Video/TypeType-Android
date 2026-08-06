package dev.typetype.android.feature.settings.content

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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
        scrollTo("Confidence")
        composeRule.onNodeWithText("Confidence").assertIsDisplayed()
        scrollTo("Community, then neutral frame")
        composeRule.onNodeWithText("Community, then neutral frame")
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
    fun toggleExposesItsLabelAndActionAsOneControl() {
        show(ContentSettingsState(isLoading = false))

        composeRule.onNode(
            isToggleable() and hasText("DeArrow titles and thumbnails"),
        ).assertHasClickAction()
    }

    @Test
    fun controlsStayDisabledUntilTheServerSettingsLoad() {
        show(ContentSettingsState(isLoading = true))

        composeRule.onAllNodes(isToggleable())[0].assertIsNotEnabled()
    }

    @Test
    fun hideEverythingRequiresConfirmationBeforeUpdatingTheServer() {
        val action = AtomicReference<ContentSettingsAction>()
        show(ContentSettingsState(isLoading = false)) { action.set(it) }

        scrollTo("Hide everything")
        composeRule.onNodeWithText("Hide everything").performClick()
        composeRule.onNodeWithText("Hide every content surface?").assertIsDisplayed()
        composeRule.onNodeWithText("Confirm").performClick()

        assertEquals(ContentSettingsAction.SetAllHidden(true), action.get())
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

    private fun scrollTo(text: String) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
    }
}
