package dev.typetype.android.feature.player.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.media3.common.PlaybackException
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlaybackFailureOverlayTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mediaDeliveryFailureRemainsActionableAtLargeFont() {
        val retried = AtomicBoolean(false)
        val backedOut = AtomicBoolean(false)
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f),
            ) {
                TypeTypeTheme {
                    PlaybackFailureOverlay(
                        error = PlaybackException(
                            "HTTP failure",
                            null,
                            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                        ),
                        onRetry = { retried.set(true) },
                        onOpenAccounts = {},
                        onBack = { backedOut.set(true) },
                    )
                }
            }
        }

        composeRule.onNodeWithText("Couldn't load this video").assertIsDisplayed()
        composeRule.onNodeWithText(
            "This instance could not deliver the media. Retry to request a fresh source.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Go back").assertIsDisplayed().performClick()

        assertTrue(retried.get())
        assertTrue(backedOut.get())
    }
}
