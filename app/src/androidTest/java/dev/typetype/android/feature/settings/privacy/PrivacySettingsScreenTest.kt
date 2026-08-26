package dev.typetype.android.feature.settings.privacy

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PrivacySettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun enabledTrackingControlReportsTheRequestedServerValue() {
        val requested = AtomicReference<Boolean>()
        show(PrivacyState(watchHistoryTrackingControlEnabled = true)) {
            requested.set(it)
        }

        composeRule.onNode(isToggleable())
            .assertIsEnabled()
            .assertIsOn()
            .performClick()

        assertEquals(false, requested.get())
    }

    @Test
    fun trackingToggleExposesItsLabelAndActionAsOneControl() {
        show(PrivacyState(watchHistoryTrackingControlEnabled = true))

        composeRule.onNode(
            isToggleable() and hasText("Watch history tracking"),
        ).assertHasClickAction()
    }

    @Test
    fun trackingControlStaysDisabledUntilServerSettingsLoad() {
        show(
            PrivacyState(
                watchHistoryTrackingEnabled = false,
                watchHistoryTrackingControlEnabled = false,
            ),
        )

        composeRule.onNode(isToggleable()).assertIsNotEnabled().assertIsOff()
    }

    @Test
    fun subscriptionFailureExplainsWhichInformationIsUnavailable() {
        show(
            PrivacyState(
                errorMessage = "The server could not refresh this value",
                failureAction = PrivacyFailureAction.LoadSubscriptions,
            ),
        )

        composeRule.onNodeWithText("Subscription count unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("The server could not refresh this value").assertIsDisplayed()
    }

    @Test
    fun searchHistoryDoesNotExposeAnUnavailableCount() {
        show(PrivacyState())

        composeRule.onNodeWithText("Clear saved searches from this account").assertIsDisplayed()
        composeRule.onNodeWithText("Search history count unavailable").assertDoesNotExist()
    }

    private fun show(
        state: PrivacyState,
        onSetWatchHistoryTracking: (Boolean) -> Unit = {},
    ) {
        composeRule.setContent {
            TypeTypeTheme {
                PrivacySettingsScreen(
                    state = state,
                    onNavigateBack = {},
                    onSetWatchHistoryTracking = onSetWatchHistoryTracking,
                    onClearWatchHistory = {},
                    onClearSearchHistory = {},
                    onUnsubscribeAll = {},
                    onDeviceNameChange = {},
                )
            }
        }
    }
}
