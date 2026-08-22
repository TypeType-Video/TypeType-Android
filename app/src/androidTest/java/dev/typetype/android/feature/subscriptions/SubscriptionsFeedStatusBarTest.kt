package dev.typetype.android.feature.subscriptions

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.typetype.android.core.ui.theme.TypeTypeTheme
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SubscriptionsFeedStatusBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cachedFailureCanBeRetriedImmediately() {
        val retried = AtomicBoolean(false)
        composeRule.setContent {
            TypeTypeTheme {
                SubscriptionsFeedStatusBar(
                    isRefreshing = false,
                    isServerRefreshing = false,
                    errorMessage = "Subscriptions are temporarily unavailable",
                    requestId = "request-refresh",
                    hasContent = true,
                    onRetry = { retried.set(true) },
                )
            }
        }

        composeRule.onNodeWithText("Subscriptions are temporarily unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("Request request-refresh").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed().performClick()

        assertTrue(retried.get())
    }
}
