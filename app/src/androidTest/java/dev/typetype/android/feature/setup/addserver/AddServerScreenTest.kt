package dev.typetype.android.feature.setup.addserver

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AddServerScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun permanentDenialOffersAndroidSettings() {
        val settingsOpened = AtomicBoolean(false)
        composeRule.setContent {
            TypeTypeTheme {
                AddServerScreen(
                    state = AddServerState(
                        url = "http://192.168.1.20:8080",
                        localNetworkPermissionDenied = true,
                        localNetworkPermissionPermanentlyDenied = true,
                    ),
                    onAction = {},
                    onConnect = {},
                    onOpenAppSettings = { settingsOpened.set(true) },
                )
            }
        }

        composeRule.onNodeWithText("Open app settings").assertIsDisplayed().performClick()

        assertTrue(settingsOpened.get())
    }
}
