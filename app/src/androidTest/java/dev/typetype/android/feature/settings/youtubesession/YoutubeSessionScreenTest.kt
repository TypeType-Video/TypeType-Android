package dev.typetype.android.feature.settings.youtubesession

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import dev.typetype.android.domain.youtubesession.YoutubeRemoteBrowserPhase
import dev.typetype.android.domain.youtubesession.YoutubeSession
import dev.typetype.android.domain.youtubesession.YoutubeSessionStatus
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class YoutubeSessionScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun disabledInstanceExplainsWhySignInCannotStart() {
        show(YoutubeSessionState(availability = YoutubeSessionAvailability.Disabled))

        composeRule.onNodeWithText("Remote YouTube sign-in is disabled on this instance.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Connect with YouTube").assertIsNotEnabled()
    }

    @Test
    fun availableInstanceCanStartSignIn() {
        val started = AtomicBoolean(false)
        show(
            state = YoutubeSessionState(
                availability = YoutubeSessionAvailability.Available,
                isStatusLoading = false,
            ),
            onStart = { started.set(true) },
        )

        composeRule.onNodeWithText("Connect with YouTube")
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        assertTrue(started.get())
    }

    @Test
    fun connectedSessionOffersDisconnect() {
        show(
            YoutubeSessionState(
                availability = YoutubeSessionAvailability.Available,
                session = YoutubeSession(YoutubeSessionStatus.Connected, 1, 1),
                isStatusLoading = false,
            ),
        )

        composeRule.onNodeWithText("Connected").assertIsDisplayed()
        composeRule.onNodeWithText("Disconnect").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun openRemoteBrowserShowsPhaseAndCancelAction() {
        val cancelled = AtomicBoolean(false)
        show(
            state = YoutubeSessionState(
                availability = YoutubeSessionAvailability.Available,
                remoteSessionId = "session",
                remotePhase = YoutubeRemoteBrowserPhase.AwaitingLogin,
            ),
            onCancel = { cancelled.set(true) },
        )

        composeRule.onNodeWithText("Phase: waiting for sign-in").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Cancel sign-in").performScrollTo().performClick()

        assertTrue(cancelled.get())
    }

    private fun show(
        state: YoutubeSessionState,
        onStart: () -> Unit = {},
        onCancel: () -> Unit = {},
    ) {
        composeRule.setContent {
            TypeTypeTheme {
                YoutubeSessionScreen(
                    state = state,
                    onNavigateBack = {},
                    onRefresh = {},
                    onStart = onStart,
                    onInput = {},
                    onCancel = onCancel,
                    onDisconnect = {},
                    onNoticeShown = {},
                )
            }
        }
    }
}
